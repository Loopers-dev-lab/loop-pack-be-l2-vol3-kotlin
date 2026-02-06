package com.loopers.user.interfaces.api

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.user.application.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.BDDMockito.willThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("PATCH /api/v1/users/me/password - 비밀번호 수정")
@WebMvcTest(UserV1Controller::class)
class UserV1ControllerChangePasswordE2ETest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val userService: UserService,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/users/me/password"
    }

    private fun requestBody(
        currentPassword: String = "Password1!",
        newPassword: String = "NewPassword1!",
    ): String =
        """
        {
            "currentPassword": "$currentPassword",
            "newPassword": "$newPassword"
        }
        """.trimIndent()

    @Nested
    @DisplayName("유효한 인증과 유효한 요청으로 비밀번호 변경 시 200 OK를 반환한다")
    inner class WhenValidRequest {
        @Test
        @DisplayName("200 OK와 성공 메시지를 반환한다")
        fun changePassword_success_returns200WithMessage() {
            // arrange
            willDoNothing().given(userService).changePassword(
                eq("testuser1"),
                eq("Password1!"),
                any(),
            )

            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "Password1!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody()),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.meta.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.message").value("비밀번호가 변경되었습니다."))
        }
    }

    @Nested
    @DisplayName("currentPassword가 저장된 비밀번호와 불일치하면 400 Bad Request를 반환한다")
    inner class WhenCurrentPasswordMismatch {
        @Test
        @DisplayName("400 Bad Request와 USER_INVALID_PASSWORD 에러코드를 반환한다")
        fun changePassword_wrongCurrentPassword_returns400() {
            // arrange
            willThrow(CoreException(ErrorType.USER_INVALID_PASSWORD, "현재 비밀번호가 일치하지 않습니다."))
                .given(userService).changePassword(
                    eq("testuser1"),
                    eq("Password1!"),
                    any(),
                )

            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "Password1!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody(currentPassword = "WrongPassword1!")),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.meta.errorCode").value("USER_INVALID_PASSWORD"))
        }
    }

    @Nested
    @DisplayName("currentPassword와 newPassword가 동일하면 400 Bad Request를 반환한다")
    inner class WhenSamePassword {
        @Test
        @DisplayName("400 Bad Request와 USER_INVALID_PASSWORD 에러코드를 반환한다")
        fun changePassword_samePassword_returns400() {
            // arrange
            willThrow(CoreException(ErrorType.USER_INVALID_PASSWORD, "새 비밀번호는 현재 비밀번호와 달라야 합니다."))
                .given(userService).changePassword(
                    eq("testuser1"),
                    eq("Password1!"),
                    any(),
                )

            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "Password1!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody(currentPassword = "Password1!", newPassword = "Password1!")),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.meta.errorCode").value("USER_INVALID_PASSWORD"))
        }
    }

    @Nested
    @DisplayName("newPassword가 비밀번호 규칙을 위반하면 400 Bad Request를 반환한다")
    inner class WhenInvalidNewPassword {
        @Test
        @DisplayName("newPassword가 8자 미만이면 400 Bad Request를 반환한다")
        fun changePassword_shortNewPassword_returns400() {
            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "Password1!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody(newPassword = "Short1!")),
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
    inner class WhenMissingHeaders {
        @Test
        @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
        fun changePassword_missingHeader_returns400() {
            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody()),
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
    inner class WhenAuthenticationFails {
        @Test
        @DisplayName("인증 실패(잘못된 헤더 비밀번호) 시 401 Unauthorized를 반환한다")
        fun changePassword_invalidAuth_returns401() {
            // arrange
            willThrow(CoreException(ErrorType.UNAUTHORIZED))
                .given(userService).changePassword(
                    eq("testuser1"),
                    eq("WrongPassword1!"),
                    any(),
                )

            // act & assert
            mockMvc.perform(
                patch(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "WrongPassword1!")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody()),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.meta.errorCode").value("UNAUTHORIZED"))
        }
    }
}
