package com.loopers.interfaces.api.payment

import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUserInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment API", description = "결제 API")
interface PaymentApiSpec {

    @Operation(
        summary = "결제 요청",
        description = "PG 연동 결제를 요청합니다.",
    )
    fun requestPayment(
        userInfo: AuthenticatedUserInfo,
        request: PaymentDto.PaymentRequest,
    ): ApiResponse<PaymentDto.PaymentResponse>

    @Operation(
        summary = "결제 콜백 수신",
        description = "PG에서 결과 콜백을 수신합니다.",
    )
    fun handleCallback(
        request: PaymentDto.CallbackRequest,
    ): ApiResponse<Unit>

    @Operation(
        summary = "결제 상태 동기화",
        description = "orderId로 결제 상태를 조회합니다.",
    )
    fun syncPayment(
        userInfo: AuthenticatedUserInfo,
        orderId: String,
    ): ApiResponse<List<PaymentDto.PaymentResponse>>
}
