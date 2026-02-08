package com.loopers.user.interfaces.api

import com.loopers.support.api.ApiResponse
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
import org.springframework.http.MediaType
import java.time.LocalDate

@DisplayName("POST /api/v1/users - 회원가입 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1SignUpE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val SIGN_UP_ENDPOINT = "/api/v1/users"
        private const val GET_ME_ENDPOINT = "/api/v1/users/me"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUpRequest(
        loginId: String = "testuser1",
        password: String = "Password1!",
        name: String = "홍길동",
        birthDate: String = "1990-01-01",
        email: String = "test@example.com",
    ): HttpEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val body = """
            {
                "loginId": "$loginId",
                "password": "$password",
                "name": "$name",
                "birthDate": "$birthDate",
                "email": "$email"
            }
        """.trimIndent()
        return HttpEntity(body, headers)
    }

    private fun getMeRequest(
        loginId: String,
        password: String,
    ): HttpEntity<Unit> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
        return HttpEntity(Unit, headers)
    }

    @Nested
    @DisplayName("회원가입 성공 시")
    inner class WhenSignUpSuccess {
        @Test
        @DisplayName("201 Created와 가입한 loginId를 반환한다")
        fun signUp_success_returns201() {
            // arrange & act
            val response = testRestTemplate.exchange(
                SIGN_UP_ENDPOINT,
                HttpMethod.POST,
                signUpRequest(),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.SignUpResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
            )
        }

        @Test
        @DisplayName("가입 후 해당 계정으로 인증이 성공한다")
        fun signUp_thenAuthWithNewAccount_succeeds() {
            // arrange - 회원가입
            testRestTemplate.exchange(
                SIGN_UP_ENDPOINT,
                HttpMethod.POST,
                signUpRequest(),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.SignUpResponse>>() {},
            )

            // act - 가입한 계정으로 내 정보 조회
            val meResponse = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("testuser1", "Password1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.MeResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(meResponse.body?.data?.loginId).isEqualTo("testuser1") },
                { assertThat(meResponse.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(meResponse.body?.data?.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(meResponse.body?.data?.email).isEqualTo("test@example.com") },
            )
        }
    }

    @Nested
    @DisplayName("중복 loginId로 가입 시")
    inner class WhenDuplicateLoginId {
        @Test
        @DisplayName("409 Conflict를 반환한다")
        fun signUp_duplicateLoginId_returns409() {
            // arrange - 첫 번째 가입
            testRestTemplate.exchange(
                SIGN_UP_ENDPOINT,
                HttpMethod.POST,
                signUpRequest(),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.SignUpResponse>>() {},
            )

            // act - 동일 loginId로 두 번째 가입
            val response = testRestTemplate.exchange(
                SIGN_UP_ENDPOINT,
                HttpMethod.POST,
                signUpRequest(),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.SignUpResponse>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 요청 시")
    inner class WhenInvalidRequest {
        @Test
        @DisplayName("loginId가 빈 문자열이면 400 Bad Request를 반환한다")
        fun signUp_invalidRequest_returns400() {
            // arrange & act
            val response = testRestTemplate.exchange(
                SIGN_UP_ENDPOINT,
                HttpMethod.POST,
                signUpRequest(loginId = ""),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.SignUpResponse>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
