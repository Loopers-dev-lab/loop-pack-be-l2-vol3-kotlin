package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Order Admin V1 API", description = "어드민 주문 API")
interface OrderAdminV1ApiSpec {

    @Operation(summary = "주문 목록 조회", description = "전체 주문 목록을 페이징 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
        ],
    )
    fun getAllOrders(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @ParameterObject pageable: Pageable,
    ): ApiResponse<Page<OrderAdminV1Dto.OrderAdminResponse>>

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "어드민 인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "주문 없음"),
        ],
    )
    fun getOrder(
        @Parameter(description = "어드민 LDAP 인증", required = true)
        ldap: String?,
        @Parameter(description = "주문 ID", required = true)
        orderId: Long,
    ): ApiResponse<OrderAdminV1Dto.OrderAdminResponse>
}
