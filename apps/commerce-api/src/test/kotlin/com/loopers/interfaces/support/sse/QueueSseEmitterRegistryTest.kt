package com.loopers.interfaces.support.sse

import com.loopers.application.queue.QueueProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("QueueSseEmitterRegistry")
class QueueSseEmitterRegistryTest {

    private lateinit var registry: QueueSseEmitterRegistry

    @BeforeEach
    fun setUp() {
        registry = QueueSseEmitterRegistry(
            queueProperties = QueueProperties(
                maxCapacity = 50_000,
                batchSize = 18,
                tokenTtlSeconds = 300,
                throughputTps = 175,
                schedulerDelayMs = 100,
                jitterMaxMs = 0,
                sseTimeoutMs = 60_000,
            ),
        )
    }

    @Nested
    @DisplayName("register 시")
    inner class Register {

        @Test
        @DisplayName("SseEmitter를 생성하고 연결 목록에 추가한다")
        fun register_addsToConnectedUsers() {
            // act
            val emitter = registry.register(1L)

            // assert
            assertThat(emitter).isNotNull()
            assertThat(registry.connectedUserIds()).containsExactly(1L)
        }

        @Test
        @DisplayName("동일 userId로 재연결 시 새 emitter로 교체한다")
        fun register_sameUser_replacesEmitter() {
            // arrange
            val first = registry.register(1L)

            // act
            val second = registry.register(1L)

            // assert
            assertThat(first).isNotSameAs(second)
            assertThat(registry.connectedUserIds()).containsExactly(1L)
        }

        @Test
        @DisplayName("여러 사용자 등록 시 모든 userId를 반환한다")
        fun register_multipleUsers_returnsAll() {
            // act
            registry.register(1L)
            registry.register(2L)
            registry.register(3L)

            // assert
            assertThat(registry.connectedUserIds()).containsExactlyInAnyOrder(1L, 2L, 3L)
        }
    }

    @Nested
    @DisplayName("complete 시")
    inner class Complete {

        @Test
        @DisplayName("연결을 종료하고 목록에서 제거한다")
        fun complete_removesFromConnectedUsers() {
            // arrange
            registry.register(1L)

            // act
            registry.complete(1L)

            // assert
            assertThat(registry.connectedUserIds()).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 userId에 대해 예외 없이 무시한다")
        fun complete_nonExistentUser_doesNothing() {
            // act & assert — 예외 없음
            registry.complete(999L)
        }
    }

    @Nested
    @DisplayName("sendEvent 시")
    inner class SendEvent {

        @Test
        @DisplayName("연결되지 않은 userId에 이벤트 전송 시 예외 없이 무시한다")
        fun sendEvent_nonExistentUser_doesNothing() {
            // act & assert — 예외 없음
            registry.sendEvent(999L, "test", mapOf("key" to "value"))
        }
    }
}
