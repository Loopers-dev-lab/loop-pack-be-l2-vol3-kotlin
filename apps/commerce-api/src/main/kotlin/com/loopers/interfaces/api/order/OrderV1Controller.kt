package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemRequest
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.domain.order.OrderService
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderV1Controller(
    private val orderFacade: OrderFacade,
    private val orderService: OrderService,
) {
    @PostMapping
    fun createOrder(
        @RequestBody request: OrderV1Dto.CreateRequest,
        httpRequest: HttpServletRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val member = httpRequest.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val items = request.items.map { OrderItemRequest(it.productId, it.quantity) }
        val result = orderFacade.createOrder(member.memberId, items, request.couponIssueId)
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(result))
    }

    @GetMapping
    fun findMyOrders(
        httpRequest: HttpServletRequest,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startAt: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endAt: LocalDate,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<OrderV1Dto.OrderResponse>> {
        val member = httpRequest.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val result = orderService.findByUserIdAndDateRange(member.memberId, startAt, endAt, pageable)
        return ApiResponse.success(result.map { OrderV1Dto.OrderResponse.from(OrderInfo.from(it)) })
    }

    @GetMapping("/{orderId}")
    fun findById(
        @PathVariable orderId: Long,
        httpRequest: HttpServletRequest,
    ): ApiResponse<OrderV1Dto.OrderResponse> {
        val member = httpRequest.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val order = orderService.findByIdAndUserId(orderId, member.memberId)
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(OrderInfo.from(order)))
    }
}
