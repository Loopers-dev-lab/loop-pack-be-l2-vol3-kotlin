package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@Tag(name = "Payment V1 API", description = "결제 API")
interface PaymentV1ApiSpec {

    @Operation(summary = "결제 요청", description = "주문에 대한 PG 결제를 요청합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "결제 요청 접수"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "404", description = "주문 없음"),
            SwaggerApiResponse(responseCode = "409", description = "중복 결제 요청"),
            SwaggerApiResponse(responseCode = "503", description = "PG 시스템 장애"),
        ],
    )
    fun requestPayment(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        request: PaymentV1Dto.CreateRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "결제 상태 조회", description = "주문의 결제 상태를 조회합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패"),
            SwaggerApiResponse(responseCode = "403", description = "타인의 주문 조회"),
            SwaggerApiResponse(responseCode = "404", description = "결제 정보 없음"),
        ],
    )
    fun getPaymentStatus(
        @Parameter(description = "로그인 ID", required = true)
        loginId: String,
        @Parameter(description = "비밀번호", required = true)
        password: String,
        @Parameter(description = "주문 ID", required = true)
        orderId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "PG 콜백 수신", description = "PG 시스템에서 결제 결과를 콜백으로 전달합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "콜백 수신 성공"),
            SwaggerApiResponse(responseCode = "404", description = "결제 정보 없음"),
        ],
    )
    fun handleCallback(
        request: com.loopers.infrastructure.pg.PgCallbackRequest,
    ): ApiResponse<Any>
}
