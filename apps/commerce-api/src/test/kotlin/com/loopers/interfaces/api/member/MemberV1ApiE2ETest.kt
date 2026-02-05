package com.loopers.interfaces.api.member

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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    // java의 static => 클래스와 동행하는 유일한 오브젝트(런타임시 변수가 할당된다)
    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val TEST_NAME = "홍길동"
        private const val TEST_EMAIL = "test@example.com"
        private val TEST_BIRTH_DATE: LocalDate = LocalDate.of(1990, 1, 15)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createTestMember(
        loginId: String = TEST_LOGIN_ID,
        password: String = TEST_PASSWORD,
    ): MemberV1Dto.SignUpResponse {
        val request = MemberV1Dto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = TEST_NAME,
            birthDate = TEST_BIRTH_DATE,
            email = TEST_EMAIL,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api/v1/members/signup",
            HttpMethod.POST,
            HttpEntity(request),
            responseType,
        )
        return response.body!!.data!!
    }

    private fun createAuthHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    @DisplayName("POST /api/v1/members/signup")
    @Nested
    inner class SignUp {

        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        fun returnsCreatedUserInfo_whenSignUpSucceeds() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/signup",
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.id).isNotNull() },
            )
        }

        @DisplayName("이미 가입된 ID로 회원가입 시도 시, 409 CONFLICT 응답을 반환한다.")
        @Test
        fun returnsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // 먼저 한 번 가입
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            testRestTemplate.exchange(
                "/api/v1/members/signup",
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // act - 같은 ID로 다시 가입 시도
            val response = testRestTemplate.exchange(
                "/api/v1/members/signup",
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
            )
        }

        @DisplayName("비밀번호가 8자 미만일 경우, 400 BAD_REQUEST 응답을 반환한다.")
        @Test
        fun returnsBadRequest_whenPasswordTooShort() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Pass1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/signup",
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @DisplayName("로그인 ID가 10자를 초과할 경우, 400 BAD_REQUEST 응답을 반환한다.")
        @Test
        fun returnsBadRequest_whenLoginIdTooLong() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "abcdefghijk",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/signup",
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @DisplayName("이메일 형식이 올바르지 않을 경우, 400 BAD_REQUEST 응답을 반환한다.")
        @Test
        fun returnsBadRequest_whenEmailFormatInvalid() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "invalid-email",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/signup",
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

    @DisplayName("GET /api/v1/members/me")
    @Nested
    inner class GetMyInfo {

        @DisplayName("인증 헤더가 유효한 경우, 내 정보를 반환한다.")
        @Test
        fun returnsMyInfo_whenAuthHeadersAreValid() {
            // arrange
            createTestMember()
            val headers = createAuthHeaders(TEST_LOGIN_ID, TEST_PASSWORD)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(TEST_LOGIN_ID) },
                { assertThat(response.body?.data?.name).isEqualTo(TEST_NAME) },
                { assertThat(response.body?.data?.email).isEqualTo(TEST_EMAIL) },
                { assertThat(response.body?.data?.birthDate).isEqualTo(TEST_BIRTH_DATE) },
            )
        }

        @DisplayName("로그인 ID가 존재하지 않는 경우, 401 UNAUTHORIZED 응답을 반환한다.")
        @Test
        fun returnsUnauthorized_whenLoginIdNotFound() {
            // arrange
            val headers = createAuthHeaders("nonexistent", TEST_PASSWORD)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
            )
        }

        @DisplayName("비밀번호가 일치하지 않는 경우, 401 UNAUTHORIZED 응답을 반환한다.")
        @Test
        fun returnsUnauthorized_whenPasswordNotMatch() {
            // arrange
            createTestMember()
            val headers = createAuthHeaders(TEST_LOGIN_ID, "WrongPassword1!")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
            )
        }
    }
}
