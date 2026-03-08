package com.loopers.interfaces.api.order

import com.loopers.application.order.CancelOrderCriteria
import com.loopers.application.order.CreateOrderCriteria
import com.loopers.application.order.CreateOrderItemCriteria
import com.loopers.application.order.GetOrderCriteria
import com.loopers.application.order.GetOrdersCriteria
import com.loopers.application.order.UserCancelOrderUseCase
import com.loopers.application.order.UserCreateOrderUseCase
import com.loopers.application.order.UserGetOrderUseCase
import com.loopers.application.order.UserGetOrdersUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val userCreateOrderUseCase: UserCreateOrderUseCase,
    private val userCancelOrderUseCase: UserCancelOrderUseCase,
    private val userGetOrdersUseCase: UserGetOrdersUseCase,
    private val userGetOrderUseCase: UserGetOrderUseCase,
) : OrderV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @RequestBody request: OrderV1Dto.CreateOrderRequest,
    ): ApiResponse<Any> {
        val criteria = CreateOrderCriteria(
            loginId = loginId,
            items = request.items.map {
                CreateOrderItemCriteria(productId = it.productId, quantity = it.quantity)
            },
            couponId = request.couponId,
        )
        val result = userCreateOrderUseCase.execute(criteria)
        return ApiResponse.success(result)
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    override fun getOrders(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startAt: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endAt: LocalDate,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<OrderV1Dto.OrderSliceResponse> {
        val criteria = GetOrdersCriteria(
            loginId = loginId,
            startAt = startAt,
            endAt = endAt,
            page = page,
            size = size,
        )
        return userGetOrdersUseCase.execute(criteria)
            .let { OrderV1Dto.OrderSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val criteria = GetOrderCriteria(loginId = loginId, orderId = orderId)
        return userGetOrderUseCase.execute(criteria)
            .let { OrderV1Dto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PatchMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    override fun cancelOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable orderId: Long,
    ): ApiResponse<Any> {
        val criteria = CancelOrderCriteria(loginId = loginId, orderId = orderId)
        val result = userCancelOrderUseCase.execute(criteria)
        return ApiResponse.success(result)
    }
}
