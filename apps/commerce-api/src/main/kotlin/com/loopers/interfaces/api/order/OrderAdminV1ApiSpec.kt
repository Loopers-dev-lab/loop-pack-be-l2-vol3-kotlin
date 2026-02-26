package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.security.AdminHeader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Order Admin V1 API", description = "어드민 주문 관련 API입니다.")
interface OrderAdminV1ApiSpec {

    @Operation(summary = "전체 주문 목록 조회", description = "전체 주문 목록을 페이지네이션으로 조회합니다.",
        parameters = [Parameter(name = AdminHeader.HEADER_LDAP, `in` = ParameterIn.HEADER, required = true)])
    fun getOrders(page: Int, size: Int): ApiResponse<List<OrderV1Dto.OrderResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다.",
        parameters = [Parameter(name = AdminHeader.HEADER_LDAP, `in` = ParameterIn.HEADER, required = true)])
    fun getOrderDetail(orderId: Long): ApiResponse<OrderV1Dto.OrderResponse>
}
