package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Tag(name = "Payment V1 API", description = "결제 관련 사용자 API 입니다.")
interface PaymentV1ApiSpec {
    @Operation(
        summary = "결제 요청",
        description = "주문에 대한 결제를 PG에 요청합니다.",
    )
    @SwaggerResponse(responseCode = "201", description = "결제 요청 성공")
    fun requestPayment(
        loginId: String,
        loginPw: String,
        request: PaymentV1Dto.RequestPaymentRequest,
    ): ApiResponse<PaymentV1Dto.RequestPaymentResponse>

    @Operation(
        summary = "결제 상세 조회",
        description = "결제의 상세 정보를 조회합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "조회 성공")
    fun getPayment(
        loginId: String,
        loginPw: String,
        paymentId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(
        summary = "결제 상태 동기화",
        description = "PG에서 결제 상태를 조회하여 동기화합니다.",
    )
    @SwaggerResponse(responseCode = "200", description = "동기화 성공")
    fun syncPayment(
        loginId: String,
        loginPw: String,
        paymentId: Long,
    ): ApiResponse<PaymentV1Dto.SyncPaymentResponse>
}
