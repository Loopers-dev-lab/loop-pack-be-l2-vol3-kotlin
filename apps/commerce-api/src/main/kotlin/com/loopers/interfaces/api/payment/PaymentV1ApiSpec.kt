package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment V1 API", description = "결제 API 입니다.")
interface PaymentV1ApiSpec {
    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 요청합니다.")
    fun requestPayment(
        authenticatedMember: AuthenticatedMember,
        request: PaymentV1Dto.PaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(summary = "결제 콜백", description = "PG사로부터 결제 결과를 수신합니다.")
    fun callback(request: PaymentV1Dto.CallbackRequest): ApiResponse<Any>

    @Operation(summary = "결제 조회", description = "주문의 결제 정보를 조회합니다.")
    fun getPayment(
        authenticatedMember: AuthenticatedMember,
        orderId: Long,
    ): ApiResponse<List<PaymentV1Dto.PaymentResponse>>
}
