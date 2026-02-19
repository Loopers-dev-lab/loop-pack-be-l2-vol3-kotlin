package com.loopers.interfaces.api.order

import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page

@Tag(name = "Order Admin V1 API", description = "주문 관리 API")
interface OrderAdminV1ApiSpec {

    @Operation(summary = "주문 상세 조회 (관리자)", description = "관리자가 주문을 조회합니다.")
    fun getOrder(orderId: Long): ApiResponse<OrderAdminV1Dto.OrderAdminResponse>

    @Operation(summary = "전체 주문 목록 조회 (관리자)", description = "관리자가 전체 주문 목록을 조회합니다.")
    fun getOrders(page: Int, size: Int): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>>
}
