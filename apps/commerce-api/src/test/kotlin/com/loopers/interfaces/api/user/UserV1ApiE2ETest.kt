package com.loopers.interfaces.api.user

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/user/register"
        private const val ENDPOINT_GET_USER_INFO = "/api/v1/user/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/user/password"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/user/register")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보로 회원가입하면, 성공 응답과 사용자 정보를 반환한다.")
        @Test
        fun register_whenValidRequest_thenReturnsUserInfo() {
            // arrange
            val request = UserV1Dto.UserRegisterRequest(
                loginId = "testuser",
                password = "password123!",
                name = "홍길동",
                birthDate = "1990-01-01",
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserRegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") }, // masked
                { assertThat(response.body?.data?.birthDate).isEqualTo(request.birthDate) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
                { assertThat(response.body?.data?.id).isNotNull() },
            )

            // verify database
            val savedUser = userJpaRepository.findByLoginId(LoginId(request.loginId))
            assertThat(savedUser).isNotNull
        }

        @DisplayName("이미 존재하는 loginId로 회원가입하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun register_whenDuplicateLoginId_thenThrowsConflict() {
            // arrange
            val existingUser = userJpaRepository.save(
                UserModel(
                    loginId = LoginId("existinguser"),
                    password = passwordEncryptor.encrypt("password123!"),
                    name = Name("기존사용자"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("existing@example.com"),
                ),
            )

            val request = UserV1Dto.UserRegisterRequest(
                loginId = "existinguser",
                password = "password123!",
                name = "홍길동",
                birthDate = "1990-01-01",
                email = "new@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserRegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
            )
        }

        @DisplayName("잘못된 이메일 형식으로 회원가입하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun register_whenInvalidEmail_thenThrowsBadRequest() {
            // arrange
            val request = UserV1Dto.UserRegisterRequest(
                loginId = "testuser",
                password = "password123!",
                name = "홍길동",
                birthDate = "1990-01-01",
                email = "invalid-email",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserRegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }
    }

    @DisplayName("GET /api/v1/user/me")
    @Nested
    inner class GetUserInfo {
        @DisplayName("유효한 인증 정보로 요청하면, 사용자 정보를 반환한다.")
        @Test
        fun getUserInfo_whenValidAuth_thenReturnsUserInfo() {
            // arrange
            val password = "password123!"
            val user = userJpaRepository.save(
                UserModel(
                    loginId = LoginId("testuser"),
                    password = passwordEncryptor.encrypt(password),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("test@example.com"),
                ),
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", password)
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_GET_USER_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(user.id) },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") }, // masked
                { assertThat(response.body?.data?.birthDate).isEqualTo("1990-01-01") },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("인증 헤더 없이 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun getUserInfo_whenNoAuthHeader_thenThrowsUnauthorized() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_GET_USER_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun getUserInfo_whenWrongPassword_thenThrowsUnauthorized() {
            // arrange
            userJpaRepository.save(
                UserModel(
                    loginId = LoginId("testuser"),
                    password = passwordEncryptor.encrypt("password123!"),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("test@example.com"),
                ),
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", "wrongpassword")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_GET_USER_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 사용자로 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun getUserInfo_whenUserNotFound_thenThrowsUnauthorized() {
            // arrange
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "nonexistentuser")
                set("X-Loopers-LoginPw", "password123!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_GET_USER_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("PUT /api/v1/user/password")
    @Nested
    inner class ChangePassword {
        @DisplayName("유효한 인증 정보로 비밀번호를 변경하면, 성공 응답을 받는다.")
        @Test
        fun changePassword_whenValidAuth_thenSuccess() {
            // arrange
            val oldPassword = "password123!"
            val newPassword = "newpassword456!"
            val user = userJpaRepository.save(
                UserModel(
                    loginId = LoginId("testuser"),
                    password = passwordEncryptor.encrypt(oldPassword),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("test@example.com"),
                ),
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", oldPassword)
            }

            val request = UserV1Dto.ChangePasswordRequest(newPassword = newPassword)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            // verify password changed
            val updatedUser = userJpaRepository.findById(user.id).get()
            assertThat(passwordEncryptor.matches(newPassword, updatedUser.password)).isTrue()
            assertThat(passwordEncryptor.matches(oldPassword, updatedUser.password)).isFalse()
        }

        @DisplayName("비밀번호 변경 후, 새 비밀번호로 로그인할 수 있다.")
        @Test
        fun changePassword_afterChange_canLoginWithNewPassword() {
            // arrange
            val oldPassword = "password123!"
            val newPassword = "newpassword456!"
            userJpaRepository.save(
                UserModel(
                    loginId = LoginId("testuser"),
                    password = passwordEncryptor.encrypt(oldPassword),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("test@example.com"),
                ),
            )

            val changePasswordHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", oldPassword)
            }

            val changePasswordRequest = UserV1Dto.ChangePasswordRequest(newPassword = newPassword)

            // act - change password
            testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(changePasswordRequest, changePasswordHeaders),
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            // act - login with new password
            val newHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", newPassword)
            }

            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_GET_USER_INFO,
                HttpMethod.GET,
                HttpEntity<Any>(newHeaders),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser") },
            )
        }

        @DisplayName("인증 헤더 없이 비밀번호 변경을 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun changePassword_whenNoAuthHeader_thenThrowsUnauthorized() {
            // arrange
            val request = UserV1Dto.ChangePasswordRequest(newPassword = "newpassword456!")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 비밀번호로 비밀번호 변경을 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun changePassword_whenWrongPassword_thenThrowsUnauthorized() {
            // arrange
            userJpaRepository.save(
                UserModel(
                    loginId = LoginId("testuser"),
                    password = passwordEncryptor.encrypt("password123!"),
                    name = Name("홍길동"),
                    birthDate = BirthDate("1990-01-01"),
                    email = Email("test@example.com"),
                ),
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "testuser")
                set("X-Loopers-LoginPw", "wrongpassword")
            }

            val request = UserV1Dto.ChangePasswordRequest(newPassword = "newpassword456!")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
