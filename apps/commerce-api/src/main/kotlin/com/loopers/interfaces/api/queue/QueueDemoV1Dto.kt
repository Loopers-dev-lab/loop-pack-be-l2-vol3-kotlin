package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueExperimentProperties
import com.loopers.application.queue.QueueStrategyType
import com.loopers.interfaces.api.order.OrderV1Dto

class QueueDemoV1Dto {
    data class ConfigResponse(
        val activeStrategy: QueueStrategyType,
        val schedulerFixedDelaySeconds: Long,
        val resolvedBatchSize: Int,
        val orderGateEnabled: Boolean,
        val supportedStrategies: List<QueueStrategyType>,
    ) {
        companion object {
            fun of(
                properties: QueueExperimentProperties,
                resolvedBatchSize: Int,
                supportedStrategies: List<QueueStrategyType>,
            ): ConfigResponse {
                return ConfigResponse(
                    activeStrategy = properties.activeStrategy,
                    schedulerFixedDelaySeconds = properties.scheduler.fixedDelay.seconds,
                    resolvedBatchSize = resolvedBatchSize,
                    orderGateEnabled = properties.enforceOrderGate,
                    supportedStrategies = supportedStrategies,
                )
            }
        }
    }

    data class OrderRequest(
        val strategy: QueueStrategyType,
        val token: String,
        val productId: Long,
        val quantity: Int,
    ) {
        fun toOrderRequest(): OrderV1Dto.CreateRequest {
            return OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = productId, quantity = quantity)),
            )
        }
    }
}
