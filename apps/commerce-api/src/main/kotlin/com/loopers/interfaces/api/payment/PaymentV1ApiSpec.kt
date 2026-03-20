package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment V1 API", description = "결제 API")
interface PaymentV1ApiSpec {
    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 요청합니다.")
    fun requestPayment(
        loginId: String,
        password: String,
        req: PaymentV1Dto.PaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "PG 콜백", description = "PG 시스템의 결제 결과 콜백을 수신합니다.")
    fun handleCallback(req: PaymentV1Dto.CallbackRequest): ApiResponse<Any>

    @Operation(summary = "결제 상세 조회", description = "결제 상세 정보를 조회합니다.")
    fun getPayment(loginId: String, password: String, paymentId: Long): ApiResponse<PaymentV1Dto.PaymentResponse>
}
