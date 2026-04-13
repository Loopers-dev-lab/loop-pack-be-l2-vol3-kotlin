package com.loopers.infrastructure.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.QueueTokenBatchProcessor
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.interfaces.support.scheduler.OutboxRelayScheduler
import com.loopers.interfaces.support.scheduler.PaymentRecoveryScheduler
import com.loopers.interfaces.support.scheduler.QueueScheduler
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisQueueTokenBatchProcessorTest @Autowired constructor(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueTokenBatchProcessor: QueueTokenBatchProcessor,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var queueScheduler: QueueScheduler

    @MockitoBean
    private lateinit var paymentRecoveryScheduler: PaymentRecoveryScheduler

    @MockitoBean
    private lateinit var outboxRelayScheduler: OutboxRelayScheduler

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("popAndIssueTokens 호출 시 대기열 pop과 토큰 발급이 함께 수행된다")
    fun popAndIssueTokens_popsQueueAndIssuesTokensAtomically() {
        // arrange
        waitingQueueRepository.enter(UserId(1L), 50_000)
        waitingQueueRepository.enter(UserId(2L), 50_000)
        waitingQueueRepository.enter(UserId(3L), 50_000)

        // act
        val issuedTokens = queueTokenBatchProcessor.popAndIssueTokens(count = 2, ttlSeconds = 300)

        // assert
        assertThat(issuedTokens).hasSize(2)
        assertThat(issuedTokens.map { it.userId.value }).containsExactly(1L, 2L)
        issuedTokens.forEach { entryToken ->
            assertThat(entryTokenRepository.find(entryToken.userId)).isEqualTo(entryToken.token)
        }
        assertThat(waitingQueueRepository.count()).isEqualTo(1)
        assertThat(waitingQueueRepository.findPosition(UserId(3L))).isZero()
    }
}
