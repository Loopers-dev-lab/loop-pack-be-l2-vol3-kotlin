package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueFacade
import com.loopers.support.auth.AuthenticatedUserInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class QueueStreamControllerTest {

    @Mock
    private lateinit var queueFacade: QueueFacade

    private lateinit var controller: QueueStreamController

    @BeforeEach
    fun setUp() {
        controller = QueueStreamController(queueFacade)
    }

    @Nested
    @DisplayName("SSE 스트림 연결할 때,")
    inner class Stream {

        @Test
        @DisplayName("GET /api/v1/queue/stream 엔드포인트가 SseEmitter를 반환한다")
        fun returnsSseEmitter() {
            // arrange
            val userInfo = AuthenticatedUserInfo(
                id = 1L,
                loginId = "user1",
                name = "테스트유저",
                email = "test@test.com",
                birthday = LocalDate.of(1990, 1, 1),
            )
            val emitter = SseEmitter(300_000L)
            whenever(queueFacade.subscribe(userInfo.id)).thenReturn(emitter)

            // act
            val result = controller.stream(userInfo)

            // assert
            assertThat(result).isSameAs(emitter)
        }
    }
}
