package com.loopers.interfaces.api.v1.order

import com.loopers.application.order.CancelOrderUseCase
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.GetOrderUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.common.PageResponse
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getOrderUseCase: GetOrderUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @AuthenticatedUser authUser: AuthUser,
        @Valid @RequestBody request: CreateOrderRequest,
    ): ApiResponse<CreateOrderResponse> {
        val id = createOrderUseCase.create(authUser.id, request.toCommand())
        return ApiResponse.success(CreateOrderResponse(id))
    }

    @GetMapping
    fun getOrders(
        @AuthenticatedUser authUser: AuthUser,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startAt: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endAt: LocalDate,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<GetOrderListResponse>> {
        val result = getOrderUseCase.getAllByUserId(authUser.id, startAt, endAt, page, size)
        return ApiResponse.success(PageResponse.from(result) { GetOrderListResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getOrder(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable id: Long,
    ): ApiResponse<GetOrderDetailResponse> {
        val orderInfo = getOrderUseCase.getById(authUser.id, id)
        return ApiResponse.success(GetOrderDetailResponse.from(orderInfo))
    }

    @DeleteMapping("/{id}")
    fun cancelOrder(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        cancelOrderUseCase.cancel(authUser.id, id)
        return ApiResponse.success(null)
    }
}
