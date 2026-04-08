package com.loopers.interfaces.api.order

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.order.OrderUseCase
import com.loopers.application.queue.QueueExperimentUseCase
import com.loopers.application.queue.QueueStrategyType
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val authUseCase: AuthUseCase,
    private val orderUseCase: OrderUseCase,
    private val queueExperimentUseCase: QueueExperimentUseCase,
) : OrderV1ApiSpec {

    @PostMapping
    override fun createOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestHeader("X-Queue-Token", required = false) queueToken: String?,
        @RequestHeader("X-Queue-Strategy", required = false) queueStrategy: QueueStrategyType?,
        @Valid @RequestBody request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.DetailResponse> {
        val member = authUseCase.authenticate(loginId, password)
        queueExperimentUseCase.validateOrderEntry(member.id!!, queueToken, queueStrategy)

        return orderUseCase.createOrder(member.id!!, request.toCommand())
            .also { queueExperimentUseCase.completeOrderEntry(member.id!!, queueToken, queueStrategy) }
            .let { OrderV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{id}")
    override fun getById(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<OrderV1Dto.DetailResponse> {
        val member = authUseCase.authenticate(loginId, password)

        return orderUseCase.getById(id, member.id!!)
            .let { OrderV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/me")
    override fun getMyOrders(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<OrderV1Dto.MainResponse>> {
        val member = authUseCase.authenticate(loginId, password)

        return orderUseCase.getMyOrders(member.id!!)
            .map { OrderV1Dto.MainResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/{id}/cancel")
    override fun cancel(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        val member = authUseCase.authenticate(loginId, password)

        orderUseCase.cancel(id, member.id!!)
        return ApiResponse.success()
    }
}
