package com.loopers.user.interfaces.api

import com.loopers.support.api.ApiResponse
import com.loopers.user.infrastructure.UserEntity
import com.loopers.user.infrastructure.UserJpaRepository
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@DisplayName("PATCH /api/v1/users/me/password - 비밀번호 변경 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ChangePasswordE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val CHANGE_PASSWORD_ENDPOINT = "/api/v1/users/me/password"
        private const val GET_ME_ENDPOINT = "/api/v1/users/me"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUserInDb(
        loginId: String = "testuser1",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        email: String = "test@example.com",
    ): UserEntity {
        return userJpaRepository.save(
            UserEntity(
                id = null,
                loginId = loginId,
                password = passwordEncoder.encode(rawPassword),
                name = name,
                birthDate = birthDate,
                email = email,
            ),
        )
    }

    private fun changePasswordRequest(
        loginId: String,
        headerPassword: String,
        currentPassword: String,
        newPassword: String,
    ): HttpEntity<String> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", headerPassword)
            contentType = MediaType.APPLICATION_JSON
        }
        val body = """
            {
                "currentPassword": "$currentPassword",
                "newPassword": "$newPassword"
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
    @DisplayName("비밀번호 변경 성공 시")
    inner class WhenChangePasswordSuccess {
        @Test
        @DisplayName("200 OK와 성공 메시지를 반환한다")
        fun changePassword_success_returns200() {
            // arrange
            createUserInDb()

            // act
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                changePasswordRequest("testuser1", "Password1!", "Password1!", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.ChangePasswordResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.message).isEqualTo("비밀번호가 변경되었습니다.") },
            )
        }

        @Test
        @DisplayName("변경 후 새 비밀번호로 인증이 성공한다 (AC-8)")
        fun changePassword_thenAuthWithNewPassword_succeeds() {
            // arrange
            createUserInDb()

            // act - 비밀번호 변경
            testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                changePasswordRequest("testuser1", "Password1!", "Password1!", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.ChangePasswordResponse>>() {},
            )

            // assert - 새 비밀번호로 내 정보 조회 성공
            val meResponse = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("testuser1", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.MeResponse>>() {},
            )
            assertThat(meResponse.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("변경 후 기존 비밀번호로 인증이 실패한다 (AC-9)")
        fun changePassword_thenAuthWithOldPassword_fails() {
            // arrange
            createUserInDb()

            // act - 비밀번호 변경
            testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                changePasswordRequest("testuser1", "Password1!", "Password1!", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.ChangePasswordResponse>>() {},
            )

            // assert - 기존 비밀번호로 내 정보 조회 실패
            val meResponse = testRestTemplate.exchange(
                GET_ME_ENDPOINT,
                HttpMethod.GET,
                getMeRequest("testuser1", "Password1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.MeResponse>>() {},
            )
            assertThat(meResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("현재 비밀번호가 불일치하면")
    inner class WhenCurrentPasswordMismatch {
        @Test
        @DisplayName("400 Bad Request를 반환한다")
        fun changePassword_wrongCurrentPassword_returns400() {
            // arrange
            createUserInDb()

            // act
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                changePasswordRequest("testuser1", "Password1!", "WrongPassword1!", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.ChangePasswordResponse>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("인증 실패 시")
    inner class WhenAuthenticationFails {
        @Test
        @DisplayName("헤더 비밀번호가 틀리면 401 Unauthorized를 반환한다")
        fun changePassword_invalidHeaderPassword_returns401() {
            // arrange
            createUserInDb()

            // act
            val response = testRestTemplate.exchange(
                CHANGE_PASSWORD_ENDPOINT,
                HttpMethod.PATCH,
                changePasswordRequest("testuser1", "WrongPassword1!", "Password1!", "NewPassword1!"),
                object : ParameterizedTypeReference<ApiResponse<UserV1Dto.ChangePasswordResponse>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
