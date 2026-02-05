package com.loopers.interfaces.api

import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/users"
        private const val ENDPOINT_ME = "/api/v1/users/me"
        private const val ENDPOINT_UPDATE_PASSWORD = "/api/v1/users/me/password"

        private const val DEFAULT_USERNAME = "username"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "안유진"
        private const val DEFAULT_EMAIL = "email@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 21, 40, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(
        username: String = DEFAULT_USERNAME,
        password: String = DEFAULT_PASSWORD,
        name: String = DEFAULT_NAME,
        email: String = DEFAULT_EMAIL,
        birthDate: ZonedDateTime = DEFAULT_BIRTH_DATE,
    ): UserModel {
        val command = RegisterCommand(
            username = username,
            password = password,
            name = name,
            email = email,
            birthDate = birthDate,
        )
        return userService.register(command)
    }

    private fun createAuthHeaders(loginId: String, loginPw: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", loginPw)
        }
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보가 주어지면, 201 CREATED를 반환한다.")
        @Test
        fun returnsCreated_whenValidInfoIsProvided() {
            // arrange
            val request = UserV1Dto.RegisterRequest(
                username = DEFAULT_USERNAME,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            )

            // act
            val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request), Void::class.java)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body).isNull() },
            )
        }

        @DisplayName("중복된 아이디가 주어지면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenDuplicateUsernameIsProvided() {
            // arrange
            registerUser()

            val request = UserV1Dto.RegisterRequest(
                username = DEFAULT_USERNAME,
                password = "password5678!",
                name = "장원영",
                email = "other@loopers.com",
                birthDate = ZonedDateTime.of(2004, 8, 31, 0, 0, 0, 0, ZoneId.of("Asia/Seoul")),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("유효하지 않은 정보가 주어지면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenInvalidInfoIsProvided() {
            // arrange
            val request = UserV1Dto.RegisterRequest(
                username = "",
                password = "short",
                name = "",
                email = "invalid-email",
                birthDate = ZonedDateTime.now().plusYears(1),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    inner class GetMe {
        @DisplayName("유효한 인증 헤더가 주어지면, 200 OK 와 이름이 마스킹된 유저 정보를 반환한다.")
        @Test
        fun returnsOkWithMaskedName_whenValidAuthHeaderIsProvided() {
            // arrange
            registerUser()

            val expectedName = "안유*"
            val headers = createAuthHeaders(DEFAULT_USERNAME, DEFAULT_PASSWORD)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, HttpEntity<Any>(headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(response.body?.data?.name).isEqualTo(expectedName) },
                { assertThat(response.body?.data?.email).isEqualTo(DEFAULT_EMAIL) },
                { assertThat(response.body?.data?.birthDate).isEqualTo(DEFAULT_BIRTH_DATE) },
            )
        }

        @DisplayName("인증 헤더가 누락되면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenAuthHeaderIsMissing() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, null, responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 인증 정보가 주어지면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenInvalidCredentialsAreProvided() {
            // arrange
            registerUser()

            val wrongPassword = "wrongPassword1!"
            val headers = createAuthHeaders(DEFAULT_USERNAME, wrongPassword)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, HttpEntity<Any>(headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 사용자의 인증 정보가 주어지면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenUserDoesNotExist() {
            // arrange
            val nonExistentUsername = "nonExistentUser"
            val headers = createAuthHeaders(nonExistentUsername, DEFAULT_PASSWORD)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, HttpEntity<Any>(headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("PATCH /api/v1/users/me/password")
    @Nested
    inner class UpdatePassword {
        @DisplayName("유효한 기존 비밀번호와 새 비밀번호가 주어지면, 204 NO CONTENT 응답을 받는다.")
        @Test
        fun returnsNoContent_whenValidPasswordsAreProvided() {
            // arrange
            registerUser()

            val newPassword = "newPassword1!"
            val headers = createAuthHeaders(DEFAULT_USERNAME, DEFAULT_PASSWORD)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = DEFAULT_PASSWORD,
                newPassword = newPassword,
            )

            // act
            val response = testRestTemplate.exchange(ENDPOINT_UPDATE_PASSWORD, HttpMethod.PATCH, HttpEntity(request, headers), Void::class.java)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT) },
                { assertThat(response.body).isNull() },
            )
        }

        @DisplayName("새 비밀번호가 유효성 규칙을 위반하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordIsInvalid() {
            // arrange
            registerUser()

            val invalidNewPassword = "short"
            val headers = createAuthHeaders(DEFAULT_USERNAME, DEFAULT_PASSWORD)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = DEFAULT_PASSWORD,
                newPassword = invalidNewPassword,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_UPDATE_PASSWORD, HttpMethod.PATCH, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증 헤더가 누락되면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenAuthHeaderIsMissing() {
            // arrange
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = DEFAULT_PASSWORD,
                newPassword = "newPassword1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_UPDATE_PASSWORD, HttpMethod.PATCH, HttpEntity(request), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 인증 정보가 주어지면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenInvalidCredentialsAreProvided() {
            // arrange
            registerUser()

            val wrongPassword = "wrongPassword1!"
            val headers = createAuthHeaders(DEFAULT_USERNAME, wrongPassword)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = DEFAULT_PASSWORD,
                newPassword = "newPassword1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_UPDATE_PASSWORD, HttpMethod.PATCH, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 사용자의 인증 정보가 주어지면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenUserDoesNotExist() {
            // arrange
            val nonExistentUsername = "nonExistentUser"
            val headers = createAuthHeaders(nonExistentUsername, DEFAULT_PASSWORD)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = DEFAULT_PASSWORD,
                newPassword = "newPassword1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_UPDATE_PASSWORD, HttpMethod.PATCH, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
