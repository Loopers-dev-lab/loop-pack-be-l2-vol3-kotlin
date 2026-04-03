package com.loopers.interfaces.api.queue

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.order.OrderUseCase
import com.loopers.application.queue.QueueExperimentProperties
import com.loopers.application.queue.QueueExperimentUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.order.OrderV1Dto
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/queue-demo")
class QueueDemoV1Controller(
    private val authUseCase: AuthUseCase,
    private val orderUseCase: OrderUseCase,
    private val queueExperimentProperties: QueueExperimentProperties,
    private val queueExperimentUseCase: QueueExperimentUseCase,
) {
    @GetMapping("/config")
    fun getConfig(): ApiResponse<QueueDemoV1Dto.ConfigResponse> {
        return QueueDemoV1Dto.ConfigResponse.of(
            properties = queueExperimentProperties,
            resolvedBatchSize = queueExperimentUseCase.resolvedBatchSize(),
            supportedStrategies = queueExperimentUseCase.supportedStrategies(),
        ).let { ApiResponse.success(it) }
    }

    @PostMapping("/orders")
    fun createQueuedOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: QueueDemoV1Dto.OrderRequest,
    ): ApiResponse<OrderV1Dto.DetailResponse> {
        val member = authUseCase.authenticate(loginId, password)
        queueExperimentUseCase.validateOrderEntryForced(member.id!!, request.token, request.strategy)

        return orderUseCase.createOrder(member.id!!, request.toOrderRequest().toCommand())
            .also { queueExperimentUseCase.completeOrderEntryForced(member.id!!, request.token, request.strategy) }
            .let(OrderV1Dto.DetailResponse::from)
            .let { ApiResponse.success(it) }
    }
}
