package com.loopers.domain.queue.waiting

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.model.EnterResult
import com.loopers.domain.queue.waiting.model.QueuePosition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueuePositionTest {

    @Nested
    @DisplayName("FakeWaitingQueueRepository")
    inner class FakeRepository {

        @Test
        @DisplayName("동일 score로 진입해도 순번이 모두 다르다")
        fun `동일 score로 진입해도 순번이 모두 다르다`() {
            // Arrange
            val repository = FakeWaitingQueueRepository()

            // Act
            val positions = (1L..10L).map { userId ->
                val enterResult = repository.enter(UserId(userId), 50_000)
                assertThat(enterResult).isInstanceOf(EnterResult.Entered::class.java)
                (enterResult as EnterResult.Entered).position
            }

            // Assert
            assertThat(positions.toSet()).hasSize(10)
        }

        @Test
        @DisplayName("상한 초과 시 enter()가 null을 반환한다")
        fun enter_overCapacity_returnsNull() {
            // Arrange
            val repository = FakeWaitingQueueRepository()
            val maxCapacity = 3
            repository.enter(UserId(1L), maxCapacity)
            repository.enter(UserId(2L), maxCapacity)
            repository.enter(UserId(3L), maxCapacity)

            // Act
            val result = repository.enter(UserId(4L), maxCapacity)

            // Assert
            assertThat(result).isEqualTo(EnterResult.QueueFull)
        }

        @Test
        @DisplayName("토큰 보유 사용자가 enter() 호출 시 AlreadyHasToken을 반환한다")
        fun enter_userWithToken_returnsAlreadyHasToken() {
            // Arrange
            val entryTokenRepository = FakeEntryTokenRepository()
            val repository = FakeWaitingQueueRepository(entryTokenRepository::find)
            entryTokenRepository.issue(UserId(1L), "entry-token", 300)

            // Act
            val result = repository.enter(UserId(1L), 50_000)

            // Assert
            assertThat(result).isEqualTo(EnterResult.AlreadyHasToken)
        }
    }

    @Nested
    @DisplayName("QueuePosition 생성")
    inner class Of {

        @Test
        @DisplayName("순번과 처리량으로 예상 대기 시간을 계산한다")
        fun `순번과 처리량으로 예상 대기 시간을 계산한다`() {
            // Arrange
            val position = 350L
            val throughputTps = 175

            // Act
            val queuePosition = QueuePosition.of(position, throughputTps)

            // Assert
            assertThat(queuePosition.position).isEqualTo(350L)
            assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(2L) // 350 / 175 = 2
        }

        @Test
        @DisplayName("순번이 0이면 예상 대기 시간도 0이다")
        fun `순번이 0이면 예상 대기 시간도 0이다`() {
            // Arrange & Act
            val queuePosition = QueuePosition.of(position = 0L, throughputTps = 175)

            // Assert
            assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(0L)
        }

        @Test
        @DisplayName("처리량이 0이면 예상 대기 시간은 0이다 — 0 나누기 방지")
        fun `처리량이 0이면 예상 대기 시간은 0이다`() {
            // Arrange & Act
            val queuePosition = QueuePosition.of(position = 100L, throughputTps = 0)

            // Assert
            assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(0L)
        }

        @Test
        @DisplayName("음수 순번이면 예상 대기 시간은 0이다")
        fun `음수 순번이면 예상 대기 시간은 0이다`() {
            // Arrange & Act
            val queuePosition = QueuePosition.of(position = -175L, throughputTps = 175)

            // Assert
            assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(0L)
        }

        @Test
        @DisplayName("대규모 순번에서 예상 대기 시간을 올바르게 계산한다")
        fun `대규모 순번에서 예상 대기 시간을 올바르게 계산한다`() {
            // Arrange & Act
            val queuePosition = QueuePosition.of(position = 50_000L, throughputTps = 175)

            // Assert
            assertThat(queuePosition.estimatedWaitSeconds).isEqualTo(285L) // 50000 / 175 ≈ 285초 ≈ 4분 45초
        }
    }
}
