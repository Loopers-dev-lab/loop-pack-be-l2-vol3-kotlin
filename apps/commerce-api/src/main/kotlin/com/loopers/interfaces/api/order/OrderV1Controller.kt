package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderService
import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId

@RestController
class OrderV1Controller(
    private val userService: UserService,
    private val orderService: OrderService,
    private val orderFacade: OrderFacade,
) : OrderV1ApiSpec {

    @PostMapping("/api/v1/orders")
    override fun createOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: OrderV1Dto.CreateRequest,
    ): ApiResponse<OrderV1Dto.CreateOrderResponse> {
        val authUser = userService.authenticate(loginId, password)
        val result = orderFacade.createOrder(authUser.id, request.toCriteria(), request.couponId)
        return ApiResponse.success(OrderV1Dto.CreateOrderResponse.from(result))
    }

    @GetMapping("/api/v1/orders")
    override fun getUserOrders(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam @DateTimeFormat(pattern = "yyyyMMdd HH:mm:ss") startAt: LocalDateTime,
        @RequestParam @DateTimeFormat(pattern = "yyyyMMdd HH:mm:ss") endAt: LocalDateTime,
    ): ApiResponse<List<OrderV1Dto.OrderSummaryResponse>> {
        val authUser = userService.authenticate(loginId, password)
        val zoneId = ZoneId.of("Asia/Seoul")
        return orderService.getUserOrders(authUser.id, startAt.atZone(zoneId), endAt.atZone(zoneId))
            .map { OrderV1Dto.OrderSummaryResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/orders/{orderId}")
    override fun getOrder(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val authUser = userService.authenticate(loginId, password)
        val orderInfo = orderService.getOrderInfo(orderId)

        if (orderInfo.userId != authUser.id) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 주문만 조회할 수 있습니다.")
        }

        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo))
    }
}
