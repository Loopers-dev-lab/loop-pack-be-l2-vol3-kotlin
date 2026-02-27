package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Order Admin V1 API", description = "주문 관리자 API")
interface OrderAdminV1ApiSpec {

    @Operation(summary = "주문 목록 조회", description = "전체 주문 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
        ],
    )
    fun getOrders(page: Int, size: Int): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 주문"),
        ],
    )
    fun getOrder(orderId: Long): ApiResponse<OrderAdminV1Dto.OrderAdminResponse>
}
