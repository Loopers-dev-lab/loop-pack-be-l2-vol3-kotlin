package com.loopers.domain.queue

import com.loopers.config.QueueProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class QueueServiceTest {
    private lateinit var queueRepository: QueueRepository
    private lateinit var queueTokenRepository: QueueTokenRepository
    private lateinit var queueProperties: QueueProperties
    private lateinit var queueService: QueueService

    @BeforeEach
    fun setUp() {
        queueRepository = mock()
        queueTokenRepository = mock()
        queueProperties = QueueProperties(enabled = true, batchSize = 30, schedulerIntervalMs = 200, tokenTtlSeconds = 300)
        queueService = QueueService(queueRepository, queueTokenRepository, queueProperties)
    }

    @DisplayName("enterQueue")
    @Nested
    inner class EnterQueue {
        @DisplayName("신규 사용자가 대기열에 진입하면, 순번과 전체 대기 인원을 반환한다.")
        @Test
        fun returnsEntryInfo_whenNewUser() {
            // arrange
            whenever(queueRepository.addIfAbsent(any(), any())).thenReturn(true)
            whenever(queueRepository.getRank(1L)).thenReturn(0L)
            whenever(queueRepository.getSize()).thenReturn(1L)

            // act
            val result = queueService.enterQueue(1L)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result!!.position).isEqualTo(1L) },
                { assertThat(result!!.totalWaiting).isEqualTo(1L) },
            )
        }

        @DisplayName("이미 대기열에 있는 사용자가 진입하면, null을 반환한다.")
        @Test
        fun returnsNull_whenDuplicateUser() {
            // arrange
            whenever(queueRepository.addIfAbsent(any(), any())).thenReturn(false)

            // act
            val result = queueService.enterQueue(1L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("getPosition")
    @Nested
    inner class GetPosition {
        @DisplayName("대기열에 있는 사용자는 WAITING 상태와 순번을 반환한다.")
        @Test
        fun returnsWaiting_whenInQueue() {
            // arrange
            whenever(queueRepository.getRank(1L)).thenReturn(5L)
            whenever(queueRepository.getSize()).thenReturn(10L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.WAITING) },
                { assertThat(result.position).isEqualTo(6L) },
                { assertThat(result.totalWaiting).isEqualTo(10L) },
            )
        }

        @DisplayName("토큰이 발급된 사용자는 TOKEN_ISSUED 상태를 반환한다.")
        @Test
        fun returnsTokenIssued_whenHasToken() {
            // arrange
            whenever(queueRepository.getRank(1L)).thenReturn(null)
            whenever(queueTokenRepository.getToken(1L)).thenReturn("some-token")
            whenever(queueRepository.getSize()).thenReturn(5L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.TOKEN_ISSUED) },
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isEqualTo("some-token") },
            )
        }

        @DisplayName("대기열에 없고 토큰도 없는 사용자는 NOT_IN_QUEUE 상태를 반환한다.")
        @Test
        fun returnsNotInQueue_whenNotInQueueAndNoToken() {
            // arrange
            whenever(queueRepository.getRank(1L)).thenReturn(null)
            whenever(queueTokenRepository.getToken(1L)).thenReturn(null)
            whenever(queueRepository.getSize()).thenReturn(0L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.NOT_IN_QUEUE) },
                { assertThat(result.position).isEqualTo(0L) },
            )
        }
    }

    @DisplayName("ETA 계산")
    @Nested
    inner class EtaCalculation {
        @DisplayName("rank=0이면, 첫 번째 배치에 포함되어 1회 주기(200ms → 0초)를 반환한다.")
        @Test
        fun returnsZeroSeconds_whenFirstInQueue() {
            // arrange - rank=0, batchSize=30, interval=200ms
            // ceil((0+1)/30) = 1 batch, 1 * 200ms = 200ms → 0초
            whenever(queueRepository.getRank(1L)).thenReturn(0L)
            whenever(queueRepository.getSize()).thenReturn(1L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
        }

        @DisplayName("rank=29이면, 첫 번째 배치에 포함되어 0초를 반환한다.")
        @Test
        fun returnsZeroSeconds_whenLastInFirstBatch() {
            // arrange - rank=29, batchSize=30, interval=200ms
            // ceil((29+1)/30) = 1 batch, 1 * 200ms = 200ms → 0초
            whenever(queueRepository.getRank(1L)).thenReturn(29L)
            whenever(queueRepository.getSize()).thenReturn(30L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(0L)
        }

        @DisplayName("rank=149이면, 5번째 배치에 포함되어 1초를 반환한다.")
        @Test
        fun returnsOneSecond_whenFifthBatch() {
            // arrange - rank=149, batchSize=30, interval=200ms
            // ceil((149+1)/30) = ceil(5.0) = 5 batches, 5 * 200ms = 1000ms → 1초
            whenever(queueRepository.getRank(1L)).thenReturn(149L)
            whenever(queueRepository.getSize()).thenReturn(200L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(1L)
        }

        @DisplayName("rank=1499이면, 50번째 배치에 포함되어 10초를 반환한다.")
        @Test
        fun returnsTenSeconds_whenLargeQueue() {
            // arrange - rank=1499, batchSize=30, interval=200ms
            // ceil((1499+1)/30) = ceil(50.0) = 50 batches, 50 * 200ms = 10000ms → 10초
            whenever(queueRepository.getRank(1L)).thenReturn(1499L)
            whenever(queueRepository.getSize()).thenReturn(2000L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(10L)
        }

        @DisplayName("rank=9999이면, 초당 10000건 유입 시 약 67초를 반환한다.")
        @Test
        fun returns67Seconds_whenTenThousandInQueue() {
            // arrange - rank=9999, batchSize=30, interval=200ms
            // ceil((9999+1)/30) = ceil(333.33) = 334 batches, 334 * 200ms = 66800ms → 66초
            whenever(queueRepository.getRank(1L)).thenReturn(9999L)
            whenever(queueRepository.getSize()).thenReturn(10000L)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(66L)
        }
    }

    @DisplayName("popAndIssueTokens")
    @Nested
    inner class PopAndIssueTokens {
        @DisplayName("카운터가 임계값 미만이면, 배치 크기만큼 토큰을 발급하고 카운터를 증가시킨다.")
        @Test
        fun popsAndIssuesTokens() {
            // arrange
            whenever(queueTokenRepository.getActiveTokenCount()).thenReturn(0L)
            whenever(queueRepository.popMin(30)).thenReturn(setOf("1", "2", "3"))
            whenever(queueTokenRepository.issueToken(any(), any())).thenReturn("token")
            whenever(queueTokenRepository.incrementActiveTokenCount(3)).thenReturn(3L)

            // act
            val result = queueService.popAndIssueTokens(30)

            // assert
            assertThat(result).isEqualTo(3)
        }

        @DisplayName("대기열이 비어있으면, 0을 반환한다.")
        @Test
        fun returnsZero_whenQueueEmpty() {
            // arrange
            whenever(queueTokenRepository.getActiveTokenCount()).thenReturn(0L)
            whenever(queueRepository.popMin(30)).thenReturn(emptySet())

            // act
            val result = queueService.popAndIssueTokens(30)

            // assert
            assertThat(result).isEqualTo(0)
        }

        @DisplayName("카운터가 임계값에 도달하면, SCAN 보정 후에도 임계값 이상이면 발급하지 않는다.")
        @Test
        fun returnsZero_whenActiveTokensAtThreshold() {
            // arrange - 카운터=150, SCAN 보정 결과도 150
            whenever(queueTokenRepository.getActiveTokenCount()).thenReturn(150L)
            whenever(queueTokenRepository.countActiveTokens()).thenReturn(150L)

            // act
            val result = queueService.popAndIssueTokens(30)

            // assert
            assertThat(result).isEqualTo(0)
        }

        @DisplayName("카운터가 임계값이지만 SCAN 보정 결과 여유가 있으면, 여유분만큼 발급한다.")
        @Test
        fun issuesAfterCorrection_whenCounterDriftedHigh() {
            // arrange - 카운터=150(TTL 만료로 drift), SCAN 보정 결과=140 → 여유 10
            whenever(queueTokenRepository.getActiveTokenCount()).thenReturn(150L)
            whenever(queueTokenRepository.countActiveTokens()).thenReturn(140L)
            whenever(queueRepository.popMin(10)).thenReturn(setOf("1", "2", "3"))
            whenever(queueTokenRepository.issueToken(any(), any())).thenReturn("token")
            whenever(queueTokenRepository.incrementActiveTokenCount(3)).thenReturn(143L)

            // act
            val result = queueService.popAndIssueTokens(30)

            // assert
            assertThat(result).isEqualTo(3)
        }

        @DisplayName("카운터가 임계값에 가까우면, 남은 여유분만큼만 발급한다.")
        @Test
        fun issuesOnlyAvailableSlots_whenNearThreshold() {
            // arrange - 카운터=140 → 여유 10개
            whenever(queueTokenRepository.getActiveTokenCount()).thenReturn(140L)
            whenever(queueRepository.popMin(10)).thenReturn(setOf("1", "2", "3", "4", "5"))
            whenever(queueTokenRepository.issueToken(any(), any())).thenReturn("token")
            whenever(queueTokenRepository.incrementActiveTokenCount(5)).thenReturn(145L)

            // act
            val result = queueService.popAndIssueTokens(30)

            // assert
            assertThat(result).isEqualTo(5)
        }
    }

    @DisplayName("validateToken")
    @Nested
    inner class ValidateToken {
        @DisplayName("저장된 토큰과 일치하면 true를 반환한다.")
        @Test
        fun returnsTrue_whenTokenMatches() {
            // arrange
            whenever(queueTokenRepository.getToken(1L)).thenReturn("valid-token")

            // act & assert
            assertThat(queueService.validateToken(1L, "valid-token")).isTrue()
        }

        @DisplayName("저장된 토큰과 일치하지 않으면 false를 반환한다.")
        @Test
        fun returnsFalse_whenTokenMismatch() {
            // arrange
            whenever(queueTokenRepository.getToken(1L)).thenReturn("valid-token")

            // act & assert
            assertThat(queueService.validateToken(1L, "wrong-token")).isFalse()
        }

        @DisplayName("토큰이 없으면 false를 반환한다.")
        @Test
        fun returnsFalse_whenTokenNotExists() {
            // arrange
            whenever(queueTokenRepository.getToken(1L)).thenReturn(null)

            // act & assert
            assertThat(queueService.validateToken(1L, "any-token")).isFalse()
        }
    }
}
