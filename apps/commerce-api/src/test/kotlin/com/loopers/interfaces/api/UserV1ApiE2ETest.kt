package com.loopers.interfaces.api

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
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(
        username: String = "username1",
        password: String = "password1234!",
        name: String = "안유진",
        email: String = "email@loopers.com",
        birthDate: ZonedDateTime = ZonedDateTime.of(2003, 9, 1, 0, 0, 0, 0, ZoneId.of("Asia/Seoul")),
    ): UserModel {
        val userModel = UserModel(
            username = username,
            password = password,
            name = name,
            email = email,
            birthDate = birthDate,
        )
        return userService.register(userModel)
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
        @DisplayName("유효한 정보가 주어지면, 201 CREATED 와 유저 정보를 반환한다.")
        @Test
        fun returnsCreated_whenValidInfoIsProvided() {
            // arrange
            val request = UserV1Dto.RegisterRequest(
                username = "username1",
                password = "password1234!",
                name = "안유진",
                email = "email@loopers.com",
                birthDate = ZonedDateTime.of(2003, 9, 1, 0, 0, 0, 0, ZoneId.of("Asia/Seoul")),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.username).isEqualTo("username1") },
                { assertThat(response.body?.data?.name).isEqualTo("안유진") },
                { assertThat(response.body?.data?.email).isEqualTo("email@loopers.com") },
            )
        }

        @DisplayName("중복된 아이디가 주어지면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenDuplicateUsernameIsProvided() {
            // arrange
            registerUser()

            val request = UserV1Dto.RegisterRequest(
                username = "username1",
                password = "password5678!",
                name = "장원영",
                email = "other@loopers.com",
                birthDate = ZonedDateTime.of(2004, 8, 31, 0, 0, 0, 0, ZoneId.of("Asia/Seoul")),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

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
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

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
            val rawPassword = "password1234!"
            registerUser(password = rawPassword)
            val headers = createAuthHeaders("username1", rawPassword)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.username).isEqualTo("username1") },
                { assertThat(response.body?.data?.name).isEqualTo("안*진") },
                { assertThat(response.body?.data?.email).isEqualTo("email@loopers.com") },
            )
        }

        @DisplayName("인증 헤더가 누락되면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenAuthHeaderIsMissing() {
            // arrange & act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("잘못된 인증 정보가 주어지면, 4xx 에러 응답을 받는다.")
        @Test
        fun returnsClientError_whenInvalidCredentialsAreProvided() {
            // arrange
            registerUser()
            val headers = createAuthHeaders("username1", "wrongPassword1!")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode.is4xxClientError).isTrue()
        }
    }

    @DisplayName("PATCH /api/v1/users/me/password")
    @Nested
    inner class UpdatePassword {
        @DisplayName("유효한 기존 비밀번호와 새 비밀번호가 주어지면, 200 OK 응답을 받는다.")
        @Test
        fun returnsOk_whenValidPasswordsAreProvided() {
            // arrange
            val rawPassword = "password1234!"
            registerUser(password = rawPassword)
            val headers = createAuthHeaders("username1", rawPassword)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = rawPassword,
                newPassword = "newPassword1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_UPDATE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("기존 비밀번호가 틀리면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenCurrentPasswordIsWrong() {
            // arrange
            val rawPassword = "password1234!"
            registerUser(password = rawPassword)
            val headers = createAuthHeaders("username1", rawPassword)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = "wrongPassword1!",
                newPassword = "newPassword1!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_UPDATE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호가 유효성 규칙을 위반하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNewPasswordIsInvalid() {
            // arrange
            val rawPassword = "password1234!"
            registerUser(password = rawPassword)
            val headers = createAuthHeaders("username1", rawPassword)
            val request = UserV1Dto.UpdatePasswordRequest(
                currentPassword = rawPassword,
                newPassword = "short",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_UPDATE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
