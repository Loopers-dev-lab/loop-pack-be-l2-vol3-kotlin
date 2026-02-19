package com.loopers.interfaces.api.order.spec

import com.loopers.interfaces.api.order.dto.OrderAdminV1Dto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import org.springframework.data.domain.Page

@Tag(name = "Order Admin V1 API", description = "주문 관리 API")
interface OrderAdminV1ApiSpec {

    @Operation(summary = "주문 상세 조회 (관리자)", description = "관리자가 주문을 조회합니다.")
    fun getOrder(orderId: Long): ApiResponse<OrderAdminV1Dto.OrderAdminResponse>

    @Operation(summary = "전체 주문 목록 조회 (관리자)", description = "관리자가 전체 주문 목록을 조회합니다.")
    fun getOrders(@PositiveOrZero page: Int, @Positive @Max(100) size: Int): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>>
}
