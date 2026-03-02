package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Order Admin V1 API", description = "주문 관리 Admin API")
interface OrderAdminV1ApiSpec {

    @Operation(
        summary = "전체 주문 목록 조회",
        description = "전체 주문 목록을 페이지 단위로 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getOrders(pageable: Pageable): ApiResponse<Page<OrderAdminV1Dto.OrderResponse>>

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 ID로 주문 상세 정보를 조회합니다.",
        parameters = [
            Parameter(
                name = "X-Loopers-Ldap",
                description = "LDAP 인증 헤더 (값: loopers.admin)",
                required = true,
                schema = Schema(type = "string"),
                `in` = ParameterIn.HEADER,
            ),
        ],
    )
    fun getOrderById(@PathVariable orderId: Long): ApiResponse<OrderAdminV1Dto.OrderResponse>
}
