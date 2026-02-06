package com.loopers.user.interfaces.api

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.user.application.UserInfo
import com.loopers.user.application.UserService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@DisplayName("GET /api/v1/users/me - 내 정보 조회")
@WebMvcTest(UserV1Controller::class)
class UserV1ControllerMeTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val userService: UserService,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/users/me"
    }

    @Nested
    @DisplayName("유효한 인증으로 조회하면 사용자 정보를 반환한다")
    inner class WhenValidCredentials {
        @Test
        @DisplayName("유효한 인증 헤더로 조회 시 200 OK와 마스킹된 사용자 정보를 반환한다")
        fun getMe_success_returns200WithMaskedInfo() {
            // arrange
            given(userService.getMe("testuser1", "Password1!"))
                .willReturn(
                    UserInfo(
                        loginId = "testuser1",
                        name = "홍길*",
                        birthDate = LocalDate.of(1990, 1, 1),
                        email = "test@example.com",
                    ),
                )

            // act & assert
            mockMvc.perform(
                get(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "Password1!"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.loginId").value("testuser1"))
                .andExpect(jsonPath("$.data.name").value("홍길*"))
                .andExpect(jsonPath("$.data.birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
        }
    }

    @Nested
    @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
    inner class WhenAuthenticationFails {
        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 401 Unauthorized를 반환한다")
        fun getMe_invalidLoginId_returns401() {
            // arrange
            given(userService.getMe("nonexistent", "Password1!"))
                .willThrow(CoreException(ErrorType.UNAUTHORIZED))

            // act & assert
            mockMvc.perform(
                get(ENDPOINT)
                    .header("X-Loopers-LoginId", "nonexistent")
                    .header("X-Loopers-LoginPw", "Password1!"),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.meta.errorCode").value("UNAUTHORIZED"))
        }

        @Test
        @DisplayName("비밀번호 불일치 시 401 Unauthorized를 반환한다")
        fun getMe_wrongPassword_returns401() {
            // arrange
            given(userService.getMe("testuser1", "WrongPassword1!"))
                .willThrow(CoreException(ErrorType.UNAUTHORIZED))

            // act & assert
            mockMvc.perform(
                get(ENDPOINT)
                    .header("X-Loopers-LoginId", "testuser1")
                    .header("X-Loopers-LoginPw", "WrongPassword1!"),
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.meta.errorCode").value("UNAUTHORIZED"))
        }
    }

    @Nested
    @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
    inner class WhenMissingHeaders {
        @Test
        @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
        fun getMe_missingHeader_returns400() {
            // act & assert
            mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isBadRequest)
        }
    }
}
