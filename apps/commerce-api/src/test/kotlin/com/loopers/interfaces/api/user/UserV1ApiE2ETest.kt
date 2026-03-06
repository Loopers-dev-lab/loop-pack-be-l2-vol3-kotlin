package com.loopers.interfaces.api.user

import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_SIGN_UP = "/api/v1/users"
        private const val ENDPOINT_RETREIVE_USERINFO = "/api/v1/users"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/users/password"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    inner class SignUp {

        @Test
        @DisplayName("유효한 회원 정보를 전달하면, 회원가입에 성공하고 201 CREATED 응답을 받는다")
        fun signUpWithValidInfo() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "test123",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
            )

            val savedUser = userJpaRepository.findAll().first()
            assertAll(
                { assertThat(savedUser.loginId.value).isEqualTo(request.loginId) },
                { assertThat(savedUser.name.value).isEqualTo(request.name) },
                { assertThat(savedUser.email.value).isEqualTo(request.email) },
            )
        }

        @Test
        @DisplayName("이미 존재하는 로그인ID로 가입을 시도하면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithDuplicateLoginId() {
            // given
            val existingUser = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted("encryptedPassword123"),
                name = Name.of("test"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            userJpaRepository.save(existingUser)

            val request = UserV1Dto.SignUpRequest(
                loginId = existingUser.loginId.value,
                password = "test12345",
                name = "test1",
                birthDate = "20260102",
                email = "test1@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("loginId가 빈 문자열이면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithEmptyLoginId() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("loginId가 3자 미만이면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithTooShortLoginId() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "ab",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("loginId에 특수문자가 포함되면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithSpecialCharacterInLoginId() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "test@123",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("name이 1글자이면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithTooShortName() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "test123",
                password = "test1234",
                name = "a",
                birthDate = "20260101",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("birthDate가 유효한 날짜 형식이 아니면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithInvalidBirthDateFormat() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "test123",
                password = "test1234",
                name = "test",
                birthDate = "2026-01-01",
                email = "test@test.com",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }

        @Test
        @DisplayName("email이 이메일 형식이 아니면 400 BAD_REQUEST 응답을 받는다")
        fun signUpWithInvalidEmailFormat() {
            // given
            val request = UserV1Dto.SignUpRequest(
                loginId = "test123",
                password = "test1234",
                name = "test",
                birthDate = "20260101",
                email = "invalid-email",
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) },
            )
        }
    }

    @DisplayName("GET /api/v1/users")
    @Nested
    inner class RetrieveUserInfo {

        @Test
        @DisplayName("회원 정보를 조회할 수 있다")
        fun retrieveUserInfo() {
            // given
            val loginId = "test123"
            val password = "test1234"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = password,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", password)
            }

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfo>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_RETREIVE_USERINFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // then
            // 이름은 마지막 글자가 마스킹되어 반환됨 (테스트 → 테스*)
            assertAll(
                { assertEquals(HttpStatus.OK, response.statusCode) },
                { assertEquals(signUpRequest.loginId, response.body?.data?.loginId) },
                { assertEquals("테스*", response.body?.data?.name) },
                { assertEquals(signUpRequest.birthDate, response.body?.data?.birthDate) },
                { assertEquals(signUpRequest.email, response.body?.data?.email) },
            )
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun retrieveUserInfoWithoutAuthHeader() {
            // given - 헤더 없음

            // when
            val response = restTemplate.exchange(
                ENDPOINT_RETREIVE_USERINFO,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }

        @Test
        @DisplayName("잘못된 비밀번호로 요청하면 401 Unauthorized 응답을 받는다")
        fun retrieveUserInfoWithWrongPassword() {
            // given
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "test123",
                password = "test1234",
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "test123")
                set("X-Loopers-LoginPw", "wrongpassword")
            }

            // when
            val response = restTemplate.exchange(
                ENDPOINT_RETREIVE_USERINFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 요청하면 401 Unauthorized 응답을 받는다")
        fun retrieveUserInfoWithNonexistentUser() {
            // given
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "nonexistent")
                set("X-Loopers-LoginPw", "test1234")
            }

            // when
            val response = restTemplate.exchange(
                ENDPOINT_RETREIVE_USERINFO,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("PUT /api/v1/users/password")
    @Nested
    inner class ChangePassword {

        @Test
        @DisplayName("비밀번호를 변경할 수 있다")
        fun changePasswordSuccessfully() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"
            val newPassword = "newPass123"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.OK, response.statusCode)

            // 새 비밀번호로 로그인 가능 확인
            val newHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", newPassword)
            }
            val userInfoResponse = restTemplate.exchange(
                ENDPOINT_RETREIVE_USERINFO,
                HttpMethod.GET,
                HttpEntity<Any>(newHeaders),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserInfo>>() {},
            )
            assertEquals(HttpStatus.OK, userInfoResponse.statusCode)
        }

        @Test
        @DisplayName("기존 비밀번호가 일치하지 않으면 400 Bad Request 응답을 받는다")
        fun changePasswordWithWrongCurrentPassword() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = "wrongPassword",
                newPassword = "newPass123",
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("현재 비밀번호와 동일한 비밀번호로 변경하려고 하면 400 Bad Request 응답을 받는다")
        fun changePasswordWithSamePassword() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = currentPassword,
                newPassword = currentPassword,
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일이 포함되면 400 Bad Request 응답을 받는다")
        fun changePasswordWithBirthDateIncluded() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"
            val birthDate = "20260101"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = birthDate,
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = currentPassword,
                newPassword = "pass$birthDate",
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 400 Bad Request 응답을 받는다")
        fun changePasswordWithTooShortPassword() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = currentPassword,
                newPassword = "short1",
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("새 비밀번호에 한글이 포함되면 400 Bad Request 응답을 받는다")
        fun changePasswordWithKoreanCharacters() {
            // given
            val loginId = "test123"
            val currentPassword = "test1234"

            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = loginId,
                password = currentPassword,
                name = "테스트",
                birthDate = "20260101",
                email = "test@test.com",
            )
            restTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(signUpRequest),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", loginId)
                set("X-Loopers-LoginPw", currentPassword)
            }

            val passwordChangeRequest = UserV1Dto.PasswordChangeRequest(
                currentPassword = currentPassword,
                newPassword = "pass한글포함123",
            )

            // when
            val response = restTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(passwordChangeRequest, headers),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }
}
