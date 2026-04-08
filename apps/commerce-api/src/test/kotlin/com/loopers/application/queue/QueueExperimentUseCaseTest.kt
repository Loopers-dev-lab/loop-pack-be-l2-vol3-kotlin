package com.loopers.application.queue

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class QueueExperimentUseCaseTest {
    @Test
    fun `요청한_전략으로_대기열_진입을_위임한다`() {
        val redisStrategy = FakeQueueStrategy(QueueStrategyType.REDIS_ONLY)
        val kafkaStrategy = FakeQueueStrategy(QueueStrategyType.KAFKA_ONLY)
        val useCase = QueueExperimentUseCase(
            queueExperimentProperties = QueueExperimentProperties(activeStrategy = QueueStrategyType.REDIS_ONLY),
            queueStrategies = listOf(redisStrategy, kafkaStrategy),
        )

        val result = useCase.enter(1L, QueueStrategyType.KAFKA_ONLY)

        assertThat(result.strategy).isEqualTo(QueueStrategyType.KAFKA_ONLY)
        assertThat(redisStrategy.enteredMemberIds).isEmpty()
        assertThat(kafkaStrategy.enteredMemberIds).containsExactly(1L)
    }

    @Test
    fun `전략이_없으면_활성_전략을_사용한다`() {
        val redisStrategy = FakeQueueStrategy(QueueStrategyType.REDIS_ONLY)
        val useCase = QueueExperimentUseCase(
            queueExperimentProperties = QueueExperimentProperties(activeStrategy = QueueStrategyType.REDIS_ONLY),
            queueStrategies = listOf(redisStrategy),
        )

        val result = useCase.getStatus(7L, null)

        assertThat(result.strategy).isEqualTo(QueueStrategyType.REDIS_ONLY)
        assertThat(redisStrategy.statusLookups).containsExactly(7L)
    }

    @Test
    fun `주문게이트가_켜져있으면_토큰_없이_검증할_수_없다`() {
        val redisStrategy = FakeQueueStrategy(QueueStrategyType.REDIS_ONLY)
        val useCase = QueueExperimentUseCase(
            queueExperimentProperties = QueueExperimentProperties(enforceOrderGate = true),
            queueStrategies = listOf(redisStrategy),
        )

        assertThatThrownBy { useCase.validateOrderEntry(1L, null, QueueStrategyType.REDIS_ONLY) }
            .isInstanceOf(CoreException::class.java)
    }

    @Test
    fun `주문게이트가_켜져있으면_토큰_검증과_완료를_전략에_위임한다`() {
        val redisStrategy = FakeQueueStrategy(QueueStrategyType.REDIS_ONLY)
        val useCase = QueueExperimentUseCase(
            queueExperimentProperties = QueueExperimentProperties(enforceOrderGate = true),
            queueStrategies = listOf(redisStrategy),
        )

        useCase.validateOrderEntry(3L, "queue-token", QueueStrategyType.REDIS_ONLY)
        useCase.completeOrderEntry(3L, "queue-token", QueueStrategyType.REDIS_ONLY)

        assertThat(redisStrategy.validatedTokens).containsExactly(3L to "queue-token")
        assertThat(redisStrategy.completedTokens).containsExactly(3L to "queue-token")
    }

    @Test
    fun `지원하는_전략만_스케줄러_대상으로_노출한다`() {
        val redisStrategy = FakeQueueStrategy(QueueStrategyType.REDIS_ONLY)
        val distributedStrategy = FakeQueueStrategy(QueueStrategyType.DISTRIBUTED_LOCK)
        val useCase = QueueExperimentUseCase(
            queueExperimentProperties = QueueExperimentProperties(),
            queueStrategies = listOf(redisStrategy, distributedStrategy),
        )

        assertThat(useCase.supportedStrategies())
            .containsExactly(QueueStrategyType.REDIS_ONLY, QueueStrategyType.DISTRIBUTED_LOCK)
    }

    private class FakeQueueStrategy(
        override val type: QueueStrategyType,
    ) : OrderEntryQueueStrategy {
        val enteredMemberIds = mutableListOf<Long>()
        val statusLookups = mutableListOf<Long>()
        val validatedTokens = mutableListOf<Pair<Long, String>>()
        val completedTokens = mutableListOf<Pair<Long, String>>()

        override fun enter(memberId: Long): QueueInfo.Status {
            enteredMemberIds += memberId
            return status(memberId)
        }

        override fun getStatus(memberId: Long): QueueInfo.Status {
            statusLookups += memberId
            return status(memberId)
        }

        override fun admit(batchSize: Int): Int = batchSize

        override fun validateToken(memberId: Long, token: String) {
            validatedTokens += memberId to token
        }

        override fun complete(memberId: Long, token: String) {
            completedTokens += memberId to token
        }

        private fun status(memberId: Long): QueueInfo.Status {
            return QueueInfo.Status(
                strategy = type,
                state = QueueEntryState.WAITING,
                position = memberId,
                totalWaitingCount = memberId,
                expectedWaitSeconds = memberId,
                token = null,
                tokenExpiresAt = null,
            )
        }
    }
}
