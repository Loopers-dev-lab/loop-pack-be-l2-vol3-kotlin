package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Payment V1 API", description = "결제 API (대고객)")
interface PaymentV1ApiSpec {

    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 요청합니다. PG 시스템에 결제 요청을 전달합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "결제 요청 접수"),
            SwaggerResponse(responseCode = "400", description = "이미 결제 완료/진행 중인 주문"),
            SwaggerResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 주문"),
        ],
    )
    fun requestPayment(
        loginUser: LoginUser,
        request: PaymentV1Dto.RequestPaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "결제 조회", description = "결제 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 결제"),
        ],
    )
    fun getPayment(
        loginUser: LoginUser,
        paymentId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "주문별 결제 목록 조회", description = "주문에 대한 결제 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "403", description = "접근 권한 없음"),
            SwaggerResponse(responseCode = "404", description = "존재하지 않는 주문"),
        ],
    )
    fun getPaymentsByOrderId(
        loginUser: LoginUser,
        orderId: Long,
    ): ApiResponse<List<PaymentV1Dto.PaymentResponse>>
}
