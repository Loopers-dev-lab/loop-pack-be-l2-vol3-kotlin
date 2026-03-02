package com.loopers.interfaces.api.order

import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.GetMyOrderUseCase
import com.loopers.application.order.GetMyOrdersUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
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
@RequestMapping(ApiPaths.Orders.BASE)
class OrderV1Controller(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getMyOrdersUseCase: GetMyOrdersUseCase,
    private val getMyOrderUseCase: GetMyOrderUseCase,
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

    @GetMapping("/me")
    override fun getMyOrders(
        @CurrentUserId userId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<OrderSummaryResponse>> {
        val result = getMyOrdersUseCase.execute(userId, startDate, endDate, page, size)
        val response = PageResult.of(
            content = result.content.map { OrderSummaryResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }

    @GetMapping("/me/{orderId}")
    override fun getMyOrder(
        @CurrentUserId userId: Long,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderDetailResponse> {
        val result = getMyOrderUseCase.execute(userId, orderId)
        return ApiResponse.success(OrderDetailResponse.from(result))
    }
}
