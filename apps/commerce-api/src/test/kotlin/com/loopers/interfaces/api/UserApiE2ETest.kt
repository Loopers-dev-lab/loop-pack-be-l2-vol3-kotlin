package com.loopers.interfaces.api

import com.loopers.interfaces.api.user.UserDto
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
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/users/signup")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 회원 정보로 가입하면, 회원이 생성되고 200 OK 응답을 받는다.")
        @Test
        fun returnsOk_whenValidUserInfoIsProvided() {
            // arrange
            val request = UserDto.SignUpRequest(
                loginId = "testuser123",
                password = "Test1234!@",
                name = "홍길동",
                email = "test@example.com",
                birthday = LocalDate.of(1990, 1, 15),
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(SIGNUP_ENDPOINT, HttpMethod.POST, httpEntity, responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
            )
        }

        @DisplayName("로그인 ID가 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("로그인 ID") },
            )
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val request = mapOf(
                "loginId" to "test@user!",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("영문과 숫자만") },
            )
        }

        @DisplayName("비밀번호가 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "",
                "name" to "홍길동",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("비밀번호") },
            )
        }

        @DisplayName("이름이 빈 문자열이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "Test1234!@",
                "name" to "",
                "email" to "test@example.com",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("이름") },
            )
        }

        @DisplayName("이메일 형식이 올바르지 않으면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenEmailIsInvalid() {
            // arrange
            val request = mapOf(
                "loginId" to "testuser123",
                "password" to "Test1234!@",
                "name" to "홍길동",
                "email" to "invalid-email",
                "birthday" to "1990-01-15",
            )
            val httpEntity = HttpEntity(request, jsonHeaders())

            // act
            val response = testRestTemplate.exchange(
                SIGNUP_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.message).contains("이메일") },
            )
        }

        private fun jsonHeaders(): HttpHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
    }
}
