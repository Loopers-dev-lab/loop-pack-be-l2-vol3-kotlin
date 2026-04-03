package com.loopers.application.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class QueueExperimentUseCase(
    private val queueExperimentProperties: QueueExperimentProperties,
    queueStrategies: List<OrderEntryQueueStrategy>,
) {
    private val strategyMap = queueStrategies.associateBy { it.type }

    fun enter(memberId: Long, strategyType: QueueStrategyType?): QueueInfo.Status {
        return strategy(strategyType).enter(memberId)
    }

    fun getStatus(memberId: Long, strategyType: QueueStrategyType?): QueueInfo.Status {
        return strategy(strategyType).getStatus(memberId)
    }

    fun validateOrderEntry(memberId: Long, token: String?, strategyType: QueueStrategyType?) {
        if (!queueExperimentProperties.enabled || !queueExperimentProperties.enforceOrderGate) {
            return
        }

        validateOrderEntryForced(memberId, token, strategyType)
    }

    fun validateOrderEntryForced(memberId: Long, token: String?, strategyType: QueueStrategyType?) {
        val queueToken = token ?: throw CoreException(ErrorType.QUEUE_TOKEN_REQUIRED)
        strategy(strategyType).validateToken(memberId, queueToken)
    }

    fun completeOrderEntry(memberId: Long, token: String?, strategyType: QueueStrategyType?) {
        if (!queueExperimentProperties.enabled || !queueExperimentProperties.enforceOrderGate) {
            return
        }

        completeOrderEntryForced(memberId, token, strategyType)
    }

    fun completeOrderEntryForced(memberId: Long, token: String?, strategyType: QueueStrategyType?) {
        val queueToken = token ?: return
        strategy(strategyType).complete(memberId, queueToken)
    }

    fun admitWaiting(strategyType: QueueStrategyType): Int {
        return strategy(strategyType).admit(queueExperimentProperties.resolvedBatchSize())
    }

    fun supportedStrategies(): List<QueueStrategyType> {
        return QueueStrategyType.entries.filter(strategyMap::containsKey)
    }

    fun resolvedBatchSize(): Int = queueExperimentProperties.resolvedBatchSize()

    private fun strategy(strategyType: QueueStrategyType?): OrderEntryQueueStrategy {
        val resolvedType = strategyType ?: queueExperimentProperties.activeStrategy
        return strategyMap[resolvedType]
            ?: throw IllegalStateException("Queue strategy is not configured. strategy=$resolvedType")
    }
}
