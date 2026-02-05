package com.loopers.interfaces.api

import com.loopers.domain.user.User
import com.loopers.infrastructure.user.UserJpaRepository
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/users/register"
        private const val ENDPOINT_GET_USER_INFO = "/api/v1/users/info"
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/users/register")
    @Nested
    inner class RegisterUser {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        fun returnsUserInfo_whenRegisterUser() {
            //arrange
            val req = UserV1Dto.RegisterUserRequest(
                loginId = "testId",
                password = "testPassword",
                name = "testName",
                birth = "2026-01-31",
                email = "test@test.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val res = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(req), responseType)

            // assert
            assertAll(
                { assertThat(res.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(res.body?.data?.loginId).isEqualTo(req.loginId) },
                { assertThat(res.body?.data?.name).isEqualTo(req.name.substring(0, req.name.length - 1) + "*") },
                { assertThat(res.body?.data?.birth).isEqualTo(req.birth) },
                { assertThat(res.body?.data?.email).isEqualTo(req.email) },
            )
        }
    }

    @DisplayName("GET /api/v1/users/info")
    @Nested
    inner class GetUserInfo {
        @DisplayName("존재하는 회원의 로그인 ID와 비밀번호를 주면, 이름 마지막 글자를 *로 마스킹한 유저 정보를 응답으로 반환한다.")
        @Test
        fun returnsUserInfo_whenLoginIdExistsAndPasswordMatched() {
            // arrange
            val password = "abcd1234"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", user.loginId)
            headers.set("X-Loopers-LoginPw", password)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val res = testRestTemplate.exchange(ENDPOINT_GET_USER_INFO, HttpMethod.GET, HttpEntity<Any>(Unit, headers), responseType)

            // assert
            assertAll(
                { assertThat(res.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(res.body?.data).isNotNull() },
                { assertThat(res.body?.data?.loginId).isEqualTo(user.loginId) },
                { assertThat(res.body?.data?.name).isEqualTo(user.name.substring(0, user.name.length - 1) + "*") },
                { assertThat(res.body?.data?.birth).isEqualTo(user.birth) },
                { assertThat(res.body?.data?.email).isEqualTo(user.email) },
            )
        }

        @DisplayName("존재하지 않는 회원의 로그인 ID와 비밀번호를 주면, 404 Not Found 응답을 반환한다.")
        @Test
        fun throwsNotFound_whenLoginIdNotExists() {
            // arrange
            val loginId = "testId"
            val password = "abcd1234"
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", loginId)
            headers.set("X-Loopers-LoginPw", password)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val res = testRestTemplate.exchange(ENDPOINT_GET_USER_INFO, HttpMethod.GET, HttpEntity<Any>(Unit, headers), responseType)

            // assert
            assertAll(
                { assertThat(res.statusCode.is4xxClientError).isTrue() },
                { assertThat(res.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
            )
        }

        @DisplayName("존재하는 회원의 로그인 ID와 틀린 비밀번호를 주면, 404 Not Found 응답을 반환한다.")
        @Test
        fun throwsNotFound_whenPasswordNotMatched() {
            // arrange
            val password = "abcd1234"
            val wrongPassword = "abcd1235"
            val user = userJpaRepository.save(User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com"))
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", user.loginId)
            headers.set("X-Loopers-LoginPw", wrongPassword)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val res = testRestTemplate.exchange(ENDPOINT_GET_USER_INFO, HttpMethod.GET, HttpEntity<Any>(Unit, headers), responseType)

            // assert
            assertAll(
                { assertThat(res.statusCode.is4xxClientError).isTrue() },
                { assertThat(res.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
            )
        }
    }
}
