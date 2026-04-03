package com.loopers.application.queue

import com.loopers.domain.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("QueueService 테스트")
class QueueServiceTest {

    private lateinit var queueStore: FakeQueueStore
    private lateinit var tokenStore: FakeQueueTokenStore
    private lateinit var configStore: FakeQueueConfigStore
    private lateinit var service: QueueService

    @BeforeEach
    fun setUp() {
        queueStore = FakeQueueStore()
        tokenStore = FakeQueueTokenStore()
        configStore = FakeQueueConfigStore()
        service = QueueService(
            queueStore = queueStore,
            tokenStore = tokenStore,
            configStore = configStore,
            batchSize = 300,
            schedulerIntervalSeconds = 3,
            tokenTtlSeconds = 300,
        )
    }

    @Nested
    @DisplayName("enterQueue")
    inner class EnterQueue {

        @Test
        @DisplayName("대기열이 활성 상태이면 진입 후 순번 정보를 반환한다")
        fun `정상 진입`() {
            configStore.setEnabled(true)

            val info = service.enterQueue(1L)

            assertThat(info.position).isEqualTo(1)
            assertThat(info.totalWaiting).isEqualTo(1)
            assertThat(info.retryAfter).isEqualTo(2)
        }

        @Test
        @DisplayName("대기열이 비활성 상태이면 예외 발생")
        fun `비활성 시 예외`() {
            configStore.setEnabled(false)

            assertThatThrownBy { service.enterQueue(1L) }
                .isInstanceOf(CoreException::class.java)
        }

        @Test
        @DisplayName("이미 대기열에 있으면 기존 순번을 멱등하게 반환한다")
        fun `중복 진입 멱등`() {
            configStore.setEnabled(true)
            service.enterQueue(1L)

            val info = service.enterQueue(1L)

            assertThat(info.position).isEqualTo(1)
        }

        @Test
        @DisplayName("이미 토큰이 발급된 멤버는 토큰 정보를 반환한다")
        fun `토큰 보유 시 토큰 반환`() {
            configStore.setEnabled(true)
            tokenStore.issue(1L, "existing-token", 300)

            val info = service.enterQueue(1L)

            assertThat(info.token).isEqualTo("existing-token")
            assertThat(info.position).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getPosition")
    inner class GetPosition {

        @Test
        @DisplayName("대기열에 있는 멤버의 순번을 반환한다")
        fun `순번 조회`() {
            configStore.setEnabled(true)
            service.enterQueue(1L)
            service.enterQueue(2L)

            val info = service.getPosition(2L)

            assertThat(info.position).isEqualTo(2)
            assertThat(info.totalWaiting).isEqualTo(2)
        }

        @Test
        @DisplayName("토큰이 발급된 멤버는 position 0과 토큰을 반환한다")
        fun `토큰 발급됨`() {
            tokenStore.issue(1L, "my-token", 300)

            val info = service.getPosition(1L)

            assertThat(info.position).isEqualTo(0)
            assertThat(info.token).isEqualTo("my-token")
            assertThat(info.retryAfter).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열에도 없고 토큰도 없으면 예외 발생")
        fun `대기열에 없음`() {
            assertThatThrownBy { service.getPosition(999L) }
                .isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("validateToken")
    inner class ValidateToken {

        @Test
        @DisplayName("유효한 토큰이면 true")
        fun `유효한 토큰`() {
            tokenStore.issue(1L, "valid-token", 300)

            assertThat(service.validateToken(1L, "valid-token")).isTrue()
        }

        @Test
        @DisplayName("토큰 불일치면 false")
        fun `토큰 불일치`() {
            tokenStore.issue(1L, "valid-token", 300)

            assertThat(service.validateToken(1L, "wrong-token")).isFalse()
        }

        @Test
        @DisplayName("토큰 미존재면 false")
        fun `토큰 없음`() {
            assertThat(service.validateToken(999L, "any")).isFalse()
        }
    }

    @Nested
    @DisplayName("consumeToken")
    inner class ConsumeToken {

        @Test
        @DisplayName("토큰을 삭제한다")
        fun `��큰 소비`() {
            tokenStore.issue(1L, "token", 300)

            service.consumeToken(1L)

            assertThat(tokenStore.get(1L)).isNull()
        }
    }

    @Nested
    @DisplayName("processQueue (스케줄러가 호출하는 메서드)")
    inner class ProcessQueue {

        @Test
        @DisplayName("대기열에서 batchSize만큼 꺼내 토큰을 발급한다")
        fun `배치 처리`() {
            configStore.setEnabled(true)
            for (i in 1L..500L) {
                service.enterQueue(i)
            }

            val issued = service.processQueue()

            assertThat(issued).isEqualTo(300)
            assertThat(queueStore.size()).isEqualTo(200)
            assertThat(tokenStore.activeCount()).isEqualTo(300)
        }

        @Test
        @DisplayName("대기열이 batchSize보다 적으면 전부 처리")
        fun `대기열이 배치보다 적음`() {
            configStore.setEnabled(true)
            for (i in 1L..5L) {
                service.enterQueue(i)
            }

            val issued = service.processQueue()

            assertThat(issued).isEqualTo(5)
            assertThat(queueStore.size()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열이 비어있으면 0 반환")
        fun `빈 대기열`() {
            val issued = service.processQueue()

            assertThat(issued).isEqualTo(0)
        }
    }
}
