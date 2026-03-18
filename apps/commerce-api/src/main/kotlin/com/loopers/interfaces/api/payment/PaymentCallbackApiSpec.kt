package com.loopers.interfaces.api.payment

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Payment", description = "결제 관련 API")
interface PaymentCallbackApiSpec {

    @Operation(
        summary = "결제 초기화",
        description = "주문에 대한 결제를 초기화합니다",
    )
    fun requestPayment(
        @Parameter(description = "로그인한 사용자 ID", required = true)
        userId: Long,
        @RequestBody(description = "결제 요청 정보", required = true)
        request: PaymentV1Dto.CreatePaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse>

    @Operation(
        summary = "PG 결제 콜백",
        description = "PG(결제게이트웨이)에서 결제 완료 후 호출하는 콜백 엔드포인트",
    )
    fun handleCallback(
        @RequestBody(description = "PG 콜백 데이터", required = true)
        callbackData: PaymentCallbackDto.CallbackRequest,
    ): ApiResponse<Unit>
}
