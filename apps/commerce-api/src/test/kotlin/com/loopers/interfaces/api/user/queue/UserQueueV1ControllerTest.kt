package com.loopers.interfaces.api.user.queue

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.queue.QueueCommand
import com.loopers.application.user.queue.EntryTokenValidationUseCase
import com.loopers.application.user.queue.QueueEnterUseCase
import com.loopers.application.user.queue.QueuePositionUseCase
import com.loopers.application.user.queue.QueueResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("User Queue V1 API contract")
@WebMvcTest(UserQueueV1Controller::class)
class UserQueueV1ControllerTest
@Autowired
constructor(
    private val mockMvc: MockMvc,
    @MockitoBean private val userAuthenticateUseCase: UserAuthenticateUseCase,
    @MockitoBean private val queueEnterUseCase: QueueEnterUseCase,
    @MockitoBean private val queuePositionUseCase: QueuePositionUseCase,
    @MockitoBean private val entryTokenValidationUseCase: EntryTokenValidationUseCase,
) {
    companion object {
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val USER_ID = 1L
        private const val ENTER_ENDPOINT = "/api/v1/queue/enter"
        private const val POSITION_ENDPOINT = "/api/v1/queue/position"
    }

    @Nested
    @DisplayName("POST /api/v1/queue/enter — 대기열 진입 API")
    inner class Enter {
        @Test
        @DisplayName("대기열 진입 성공 시 200 OK와 WAITING 상태를 반환한다")
        fun enter_success() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(USER_ID)
            given(queueEnterUseCase.enter(QueueCommand.Enter(USER_ID)))
                .willReturn(
                    QueueResult.Enter.Waiting(
                        position = 0,
                        estimatedWaitSeconds = 0,
                        totalWaiting = 1,
                    ),
                )

            mockMvc.perform(
                post(ENTER_ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.position").value(0))
                .andExpect(jsonPath("$.data.estimatedWaitSeconds").value(0))
                .andExpect(jsonPath("$.data.totalWaiting").value(1))
                .andExpect(jsonPath("$.data.token").value(nullValue()))
                .andExpect(jsonPath("$.data.tokenExpiresInSeconds").value(nullValue()))
        }

        @Test
        @DisplayName("이미 토큰이 발급된 경우 200 OK와 READY 상태를 반환한다")
        fun enter_alreadyReady() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(USER_ID)
            given(queueEnterUseCase.enter(QueueCommand.Enter(USER_ID)))
                .willReturn(
                    QueueResult.Enter.Ready(
                        token = "test-token-uuid",
                        tokenExpiresInSeconds = 300,
                    ),
                )

            mockMvc.perform(
                post(ENTER_ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.token").value("test-token-uuid"))
                .andExpect(jsonPath("$.data.tokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.data.position").value(nullValue()))
        }

        @Test
        @DisplayName("인증 헤더가 누락되면 400을 반환한다")
        fun enter_unauthorized() {
            mockMvc.perform(post(ENTER_ENDPOINT))
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/queue/position — 순번 조회 API")
    inner class GetPosition {
        @Test
        @DisplayName("대기 중인 경우 200 OK와 WAITING 상태를 반환한다")
        fun getPosition_waiting() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(USER_ID)
            given(queuePositionUseCase.getPosition(QueueCommand.Position(USER_ID)))
                .willReturn(
                    QueueResult.Position.Waiting(
                        position = 50,
                        estimatedWaitSeconds = 1,
                        totalWaiting = 100,
                        retryAfterMs = 1000,
                    ),
                )

            mockMvc.perform(
                get(POSITION_ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.position").value(50))
                .andExpect(jsonPath("$.data.estimatedWaitSeconds").value(1))
                .andExpect(jsonPath("$.data.totalWaiting").value(100))
                .andExpect(jsonPath("$.data.retryAfterMs").value(1000))
                .andExpect(jsonPath("$.data.token").doesNotExist())
        }

        @Test
        @DisplayName("토큰이 발급된 경우 200 OK와 READY 상태를 반환한다 (token 필드 없음)")
        fun getPosition_ready() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(USER_ID)
            given(queuePositionUseCase.getPosition(QueueCommand.Position(USER_ID)))
                .willReturn(
                    QueueResult.Position.Ready(
                        tokenExpiresInSeconds = 300,
                    ),
                )

            mockMvc.perform(
                get(POSITION_ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.tokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.data.position").value(nullValue()))
        }

        @Test
        @DisplayName("대기열에 없는 사용자면 404를 반환한다")
        fun getPosition_notInQueue() {
            given(userAuthenticateUseCase.authenticateAndGetId(LOGIN_ID, PASSWORD)).willReturn(USER_ID)
            given(queuePositionUseCase.getPosition(QueueCommand.Position(USER_ID)))
                .willThrow(CoreException(ErrorType.QUEUE_ENTRY_NOT_FOUND))

            mockMvc.perform(
                get(POSITION_ENDPOINT)
                    .header("X-Loopers-LoginId", LOGIN_ID)
                    .header("X-Loopers-LoginPw", PASSWORD),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.meta.errorCode").value("QUEUE_ENTRY_NOT_FOUND"))
        }
    }
}
