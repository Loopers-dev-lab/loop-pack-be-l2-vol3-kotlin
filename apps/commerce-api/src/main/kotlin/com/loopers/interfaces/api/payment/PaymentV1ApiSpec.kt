package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment V1 API", description = "결제 관련 API")
interface PaymentV1ApiSpec {
    @Operation(summary = "주문 결제 요청")
    fun requestPayment(
        loginId: String,
        password: String,
        request: PaymentV1Dto.Request,
    ): ApiResponse<PaymentV1Dto.DetailResponse>

    @Operation(summary = "주문 결제 상태 동기화")
    fun syncPayment(
        loginId: String,
        password: String,
        orderId: Long,
    ): ApiResponse<PaymentV1Dto.DetailResponse>
}
