package com.loopers.interfaces.api.user.auth

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
import org.springframework.http.MediaType
import java.time.LocalDate

@DisplayName("GET /api/v1/users/me - 내 정보 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserAuthV1MeE2ETest
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

    private fun signUpViaApi(
        loginId: String = "testuser1",
        password: String = "Password1!",
        name: String = "홍길동",
        birthDate: String = "1990-01-01",
        email: String = "test@example.com",
    ) {
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
        testRestTemplate.exchange(
            SIGN_UP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(body, headers),
            object : ParameterizedTypeReference<ApiResponse<UserAuthV1Response.SignUp>>() {},
        )
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
    @DisplayName("내 정보 조회 성공 시")
    inner class WhenGetMeSuccess {
        @Test
        @DisplayName("200 OK와 내 정보를 반환한다")
        fun getMe_success_returns200() {
            // arrange
            signUpViaApi()

            // act
            val response = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("testuser1", "Password1!"),
                object : ParameterizedTypeReference<ApiResponse<UserAuthV1Response.Me>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(response.body?.data?.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }
    }

    @Nested
    @DisplayName("인증 실패 시")
    inner class WhenAuthenticationFails {
        @Test
        @DisplayName("헤더 비밀번호가 틀리면 401 Unauthorized를 반환한다")
        fun getMe_invalidPassword_returns401() {
            // arrange
            signUpViaApi()

            // act
            val response = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("testuser1", "WrongPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        @DisplayName("존재하지 않는 loginId이면 401 Unauthorized를 반환한다")
        fun getMe_nonExistentUser_returns401() {
            // act
            val response = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("nonexistent", "Password1!"),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
