package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order V1 Admin API", description = "주문 관련 어드민 API 입니다.")
interface OrderV1AdminApiSpec {
    @Operation(
        summary = "주문 목록 조회",
        description = "페이지네이션으로 주문 목록을 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getOrders(
        ldap: String,
        page: Int,
        size: Int,
    ): ApiResponse<OrderV1AdminDto.OrderSliceResponse>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문의 상세 정보를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getOrder(
        ldap: String,
        orderId: Long,
    ): ApiResponse<OrderV1AdminDto.OrderDetailResponse>
}
