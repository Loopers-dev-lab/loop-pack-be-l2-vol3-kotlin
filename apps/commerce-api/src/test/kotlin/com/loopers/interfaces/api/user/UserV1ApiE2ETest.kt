package com.loopers.interfaces.api.user

import com.loopers.domain.user.User
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions
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
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_SIGN_UP = "/api/v1/users/sign-up"
        private const val ENDPOINT_USER = "/api/v1/users/user"

        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("POST /api/v1/users/sign-up")
    inner class SignUp {

        @Test
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        fun returnsUserInfo_whenSignUpIsSuccessful() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { Assertions.assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
                { Assertions.assertThat(response.body?.data?.name).isEqualTo("홍길동") },
                { Assertions.assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }
    }

    @Nested
    @DisplayName("헤더 검증")
    inner class HeaderValidation {
        @Test
        @DisplayName("회원가입 요청은 헤더 없이도 성공한다.")
        fun signUpSucceeds_withoutHeaders() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            Assertions.assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @Test
        @DisplayName("X-Loopers-LoginId 헤더가 없으면, 400 Bad Request 응답을 반환한다.")
        fun returnsBadRequest_whenLoginIdHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_PW, "Password1!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is4xxClientError).isTrue() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @Test
        @DisplayName("X-Loopers-LoginPw 헤더가 없으면, 400 Bad Request 응답을 반환한다.")
        fun returnsBadRequest_whenLoginPwHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser1")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is4xxClientError).isTrue() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @Test
        @DisplayName("두 헤더 모두 없으면, 400 Bad Request 응답을 반환한다.")
        fun returnsBadRequest_whenBothHeadersAreMissing() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(HttpHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is4xxClientError).isTrue() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/user")
    inner class GetUserInfo {

        @Test
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        fun returnsUserInfo_whenGetMeIsSuccessful() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userJpaRepository.save(user)

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser1")
                set(HEADER_LOGIN_PW, "Password1!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { Assertions.assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
                { Assertions.assertThat(response.body?.data?.name).isEqualTo("홍길*") },  // 마스킹된 이름
            )
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면, 401 Unauthorized 응답을 반환한다.")
        fun returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            val user = User(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            userJpaRepository.save(user)

            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "testuser1")
                set(HEADER_LOGIN_PW, "WrongPassword!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is4xxClientError).isTrue() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        fun returnsNotFound_whenUserDoesNotExist() {
            // arrange
            val headers = HttpHeaders().apply {
                set(HEADER_LOGIN_ID, "nonexistent")
                set(HEADER_LOGIN_PW, "Password1!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_USER,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { Assertions.assertThat(response.statusCode.is4xxClientError).isTrue() },
                { Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
            )
        }
    }
}
