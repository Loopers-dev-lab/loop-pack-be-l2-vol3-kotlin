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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
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
}
