package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.HttpHeaders
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders as SpringHttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_SIGNUP = "/api/v1/users"
        private const val ENDPOINT_ME = "/api/v1/users/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/users/me/password"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class `회원가입 시` {

        @Test
        fun `올바른 정보로 회원가입하면 200 OK와 유저 정보를 반환한다`() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGNUP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },  // 이름 마스킹 적용
                { assertThat(response.body?.data?.birthday).isEqualTo(LocalDate.of(1990, 1, 15)) },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `이미 존재하는 loginId로 회원가입하면 409 CONFLICT를 반환한다`() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            // 첫 번째 회원가입
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, request)

            // act - 같은 loginId로 다시 회원가입
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGNUP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `비밀번호 규칙 위반 시 400 BAD_REQUEST를 반환한다`() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "short",  // 규칙 위반
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGNUP,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    inner class `내 정보 조회 시` {

        @Test
        fun `올바른 인증 정보로 조회하면 200 OK와 유저 정보를 반환한다`() {
            // arrange
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "Abcd1234!")
            }

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
                { assertThat(response.body?.data?.loginId).isEqualTo("testuser1") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },  // 이름 마스킹 적용
                { assertThat(response.body?.data?.birthday).isEqualTo(LocalDate.of(1990, 1, 15)) },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `존재하지 않는 loginId로 조회하면 401 UNAUTHORIZED를 반환한다`() {
            // arrange
            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "nonexistent")
                set(HttpHeaders.LOGIN_PW, "Abcd1234!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `잘못된 비밀번호로 조회하면 401 UNAUTHORIZED를 반환한다`() {
            // arrange
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "WrongPassword!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    inner class `비밀번호 변경 시` {

        @Test
        fun `올바른 인증 정보로 비밀번호를 변경하면 200 OK를 반환한다`() {
            // arrange
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "Abcd1234!")
            }
            val changePasswordRequest = UserV1Dto.ChangePasswordRequest(
                newPassword = "NewPass5678!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(changePasswordRequest, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            // verify - 새 비밀번호로 로그인 가능한지 확인
            val newHeaders = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "NewPass5678!")
            }
            val verifyResponse = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(newHeaders),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {},
            )
            assertThat(verifyResponse.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        fun `잘못된 인증 정보로 비밀번호 변경 시 401 UNAUTHORIZED를 반환한다`() {
            // arrange
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "WrongPassword!")
            }
            val changePasswordRequest = UserV1Dto.ChangePasswordRequest(
                newPassword = "NewPass5678!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(changePasswordRequest, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `새 비밀번호가 규칙에 맞지 않으면 400 BAD_REQUEST를 반환한다`() {
            // arrange
            val signUpRequest = UserV1Dto.SignUpRequest(
                loginId = "testuser1",
                password = "Abcd1234!",
                name = "홍길동",
                birthday = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

            val headers = SpringHttpHeaders().apply {
                set(HttpHeaders.LOGIN_ID, "testuser1")
                set(HttpHeaders.LOGIN_PW, "Abcd1234!")
            }
            val changePasswordRequest = UserV1Dto.ChangePasswordRequest(
                newPassword = "short",  // 규칙 위반
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(changePasswordRequest, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
