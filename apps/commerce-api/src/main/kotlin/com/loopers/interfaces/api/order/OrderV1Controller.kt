package com.loopers.interfaces.api.order

import com.loopers.application.order.CreateOrderUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.constant.ApiPaths
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Orders.BASE)
class OrderV1Controller(
    private val createOrderUseCase: CreateOrderUseCase,
) : OrderV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createOrder(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: OrderCreateRequest,
    ): ApiResponse<OrderCreateResponse> {
        val orderInfo = createOrderUseCase.execute(request.toCommand(userId))
        return ApiResponse.success(OrderCreateResponse.from(orderInfo))
    }

    override fun getMyOrders(userId: Long, startDate: String, endDate: String, page: Int, size: Int) =
        throw UnsupportedOperationException("Implemented in Commit 3")

    override fun getMyOrder(userId: Long, orderId: Long) =
        throw UnsupportedOperationException("Implemented in Commit 3")
}
