package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QueueServiceTest {

    private lateinit var queueRepository: FakeQueueRepository
    private lateinit var tokenRepository: FakeTokenRepository
    private lateinit var queueService: QueueService

    private val properties = QueueProperties(
        maxQueueSize = 100,
        tokenTtlSeconds = 300,
        batchSize = 5,
        maxActiveTokens = 10,
        throughputPerSecond = 10.0,
    )

    @BeforeEach
    fun setUp() {
        queueRepository = FakeQueueRepository()
        tokenRepository = FakeTokenRepository()
        queueService = QueueService(queueRepository, tokenRepository, properties)
    }

    @Nested
    @DisplayName("enterQueue")
    inner class EnterQueue {

        @Test
        @DisplayName("대기열에 진입하면 순번이 반환된다")
        fun enterQueueReturnsPosition() {
            // arrange & act
            val result = queueService.enterQueue(1L)

            // assert — 액티브 토큰 여유 있으므로 바로 토큰 발급 (position=0)
            assertThat(result.position).isEqualTo(0)
        }

        @Test
        @DisplayName("액티브 토큰이 가득 차면 대기열에 진입한다")
        fun enterQueueWhenActiveTokensFull() {
            // arrange — 10개 토큰을 미리 발급
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }

            // act
            val result = queueService.enterQueue(11L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(1) },
                { assertThat(result.alreadyInQueue).isFalse() },
                { assertThat(result.estimatedWaitSeconds).isGreaterThan(0) },
            )
        }

        @Test
        @DisplayName("이미 대기열에 있으면 기존 순번을 반환한다")
        fun enterQueueWhenAlreadyInQueue() {
            // arrange — 토큰 가득 채우고, 11번 유저 진입
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            queueService.enterQueue(11L)

            // act
            val result = queueService.enterQueue(11L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(1) },
                { assertThat(result.alreadyInQueue).isTrue() },
            )
        }

        @Test
        @DisplayName("이미 토큰이 발급된 유저가 다시 진입하면 position 0을 반환한다")
        fun enterQueueWhenAlreadyHasToken() {
            // arrange
            tokenRepository.save(1L, "existing-token", 300)

            // act
            val result = queueService.enterQueue(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0) },
                { assertThat(result.alreadyInQueue).isTrue() },
            )
        }

        @Test
        @DisplayName("대기열이 가득 차면 QUEUE_FULL 예외가 발생한다")
        fun enterQueueWhenQueueFull() {
            // arrange — 토큰 가득 채우고, 대기열도 가득 채움
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            for (i in 11L..110L) {
                queueRepository.add(i)
            }

            // act & assert
            val exception = assertThrows<CoreException> {
                queueService.enterQueue(111L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.QUEUE_FULL)
        }

        @Test
        @DisplayName("진입 순서가 보장된다")
        fun enterQueueMaintainsOrder() {
            // arrange — 토큰 가득 채움
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }

            // act
            val result1 = queueService.enterQueue(11L)
            val result2 = queueService.enterQueue(12L)
            val result3 = queueService.enterQueue(13L)

            // assert
            assertAll(
                { assertThat(result1.position).isEqualTo(1) },
                { assertThat(result2.position).isEqualTo(2) },
                { assertThat(result3.position).isEqualTo(3) },
            )
        }
    }

    @Nested
    @DisplayName("getPosition")
    inner class GetPosition {

        @Test
        @DisplayName("대기열에 있는 유저의 순번을 조회한다")
        fun getPositionReturnsCurrentPosition() {
            // arrange
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            queueService.enterQueue(11L)
            queueService.enterQueue(12L)

            // act
            val result = queueService.getPosition(12L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(2) },
                { assertThat(result.token).isNull() },
                { assertThat(result.totalWaiting).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("토큰이 발급된 유저는 position 0과 토큰을 반환한다")
        fun getPositionWhenTokenIssued() {
            // arrange
            tokenRepository.save(1L, "my-token", 300)

            // act
            val result = queueService.getPosition(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0) },
                { assertThat(result.token).isEqualTo("my-token") },
            )
        }

        @Test
        @DisplayName("대기열에 없는 유저를 조회하면 NOT_FOUND 예외가 발생한다")
        fun getPositionWhenNotInQueue() {
            // act & assert
            val exception = assertThrows<CoreException> {
                queueService.getPosition(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("순번에 따라 retryAfterMs가 달라진다")
        fun getPositionRetryAfterMsVariesByPosition() {
            // arrange — 토큰 가득 채움 + 다수 유저 대기
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            // 50번째 유저와 200번째 유저
            for (i in 11L..210L) {
                queueRepository.add(i)
            }

            // act
            val close = queueService.getPosition(60L) // 50번째
            val far = queueService.getPosition(210L) // 200번째

            // assert
            assertAll(
                { assertThat(close.retryAfterMs).isEqualTo(1_000) },
                { assertThat(far.retryAfterMs).isEqualTo(3_000) },
            )
        }
    }

    @Nested
    @DisplayName("processQueue")
    inner class ProcessQueue {

        @BeforeEach
        fun enableQueue() {
            queueRepository.enable()
        }

        @Test
        @DisplayName("대기열에서 N명을 꺼내 토큰을 발급한다")
        fun processQueueIssuesTokens() {
            // arrange
            for (i in 1L..10L) {
                queueRepository.add(i)
            }

            // act
            val issued = queueService.processQueue(5)

            // assert
            assertAll(
                { assertThat(issued).isEqualTo(5) },
                { assertThat(queueRepository.size()).isEqualTo(5) },
                { assertThat(tokenRepository.get(1L)).isNotNull() },
                { assertThat(tokenRepository.get(5L)).isNotNull() },
                { assertThat(tokenRepository.get(6L)).isNull() },
            )
        }

        @Test
        @DisplayName("활성 토큰이 maxActiveTokens에 도달하면 추가 발급하지 않는다")
        fun processQueueRespectsMaxActiveTokens() {
            // arrange — 이미 10개 활성
            for (i in 1L..10L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            for (i in 11L..20L) {
                queueRepository.add(i)
            }

            // act
            val issued = queueService.processQueue(5)

            // assert
            assertThat(issued).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열이 비어있으면 0을 반환한다")
        fun processQueueWhenEmpty() {
            // act
            val issued = queueService.processQueue(5)

            // assert
            assertThat(issued).isEqualTo(0)
        }

        @Test
        @DisplayName("토큰 만료로 여유가 생기면 그만큼 추가 발급한다")
        fun processQueueIssuesForExpiredSlots() {
            // arrange — 8개 활성 → 2개 여유
            for (i in 1L..8L) {
                tokenRepository.save(i, "token-$i", 300)
            }
            for (i in 11L..20L) {
                queueRepository.add(i)
            }

            // act
            val issued = queueService.processQueue(5)

            // assert
            assertThat(issued).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("recoverExpiredProcessing")
    inner class RecoverExpiredProcessing {

        /**
         * 각 테스트마다 독립된 (fakeQueue, service) 쌍을 직접 구성한다.
         * processingTimeoutSeconds=0 → moveToProcessing 직후 바로 만료로 판단 (sleep 불필요)
         */

        @Test
        @DisplayName("서버 장애로 processing에 남은 항목을 waiting으로 복구한다")
        fun recoverItemsFromProcessingToWaiting() {
            // arrange — processing 단계에서 서버 장애 시뮬레이션 (직접 moveToProcessing 호출)
            val fakeQueue = FakeQueueRepository().also { it.enable() }
            val fakeToken = FakeTokenRepository()
            val service = QueueService(fakeQueue, fakeToken, properties.copy(processingTimeoutSeconds = 0L))

            for (i in 1L..5L) {
                fakeQueue.add(i)
            }
            // 3명을 processing으로 이동 (토큰 발급 전 서버 죽은 상황)
            fakeQueue.moveToProcessing(3L)

            // act
            val recovered = service.recoverExpiredProcessing()

            // assert
            assertAll(
                { assertThat(recovered).isEqualTo(3) },
                // 나머지 2명(대기) + 복구된 3명 = 5명
                { assertThat(fakeQueue.size()).isEqualTo(5) },
                // 토큰은 발급되지 않은 상태여야 함
                { assertThat(fakeToken.get(1L)).isNull() },
            )
        }

        @Test
        @DisplayName("이미 재진입한 유저는 복구 시 순번이 덮어써지지 않는다")
        fun recoverDoesNotOverwriteReEnteredUser() {
            // arrange
            val fakeQueue = FakeQueueRepository().also { it.enable() }
            val service = QueueService(fakeQueue, FakeTokenRepository(), properties.copy(processingTimeoutSeconds = 0L))

            fakeQueue.add(1L)
            fakeQueue.moveToProcessing(1L) // 1번 유저 processing으로 이동

            // 서버 장애 중에 유저가 직접 재진입
            fakeQueue.add(1L)
            assertThat(fakeQueue.size()).isEqualTo(1) // 재진입 성공

            // act — 복구 시도
            val recovered = service.recoverExpiredProcessing()

            // assert — 복구는 1건이지만, 이미 재진입해 있으므로 중복되지 않음
            assertAll(
                { assertThat(recovered).isEqualTo(1) },
                { assertThat(fakeQueue.size()).isEqualTo(1) }, // 중복 없이 1명
            )
        }

        @Test
        @DisplayName("대기열이 비활성화 상태면 복구를 수행하지 않는다")
        fun recoverSkipsWhenQueueDisabled() {
            // arrange
            val fakeQueue = FakeQueueRepository() // enable 안 함
            val service = QueueService(fakeQueue, FakeTokenRepository(), properties.copy(processingTimeoutSeconds = 0L))

            for (i in 1L..3L) {
                fakeQueue.add(i)
            }
            fakeQueue.moveToProcessing(3L)

            // act
            val recovered = service.recoverExpiredProcessing()

            // assert
            assertThat(recovered).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("validateAndConsumeToken")
    inner class ValidateAndConsumeToken {

        @Test
        @DisplayName("유효한 토큰이면 소비에 성공한다")
        fun validateAndConsumeValidToken() {
            // arrange
            tokenRepository.save(1L, "valid-token", 300)

            // act & assert (예외 없이 통과)
            queueService.validateAndConsumeToken(1L, "valid-token")

            // 소비 후 토큰이 삭제되었는지 확인
            assertThat(tokenRepository.get(1L)).isNull()
        }

        @Test
        @DisplayName("토큰이 없으면 INVALID_TOKEN 예외가 발생한다")
        fun validateAndConsumeWhenNoToken() {
            // act & assert
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(1L, "some-token")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_TOKEN)
        }

        @Test
        @DisplayName("토큰이 불일치하면 INVALID_TOKEN 예외가 발생한다")
        fun validateAndConsumeWhenTokenMismatch() {
            // arrange
            tokenRepository.save(1L, "correct-token", 300)

            // act & assert
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(1L, "wrong-token")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_TOKEN)
        }

        @Test
        @DisplayName("같은 토큰으로 두 번 소비하면 두 번째는 실패한다")
        fun validateAndConsumeTwiceFails() {
            // arrange
            tokenRepository.save(1L, "one-time-token", 300)

            // act — 첫 번째 성공
            queueService.validateAndConsumeToken(1L, "one-time-token")

            // assert — 두 번째 실패
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(1L, "one-time-token")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_TOKEN)
        }
    }
}
