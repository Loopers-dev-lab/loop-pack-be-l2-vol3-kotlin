package com.loopers.interfaces.api.payment.spec

import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid

@Tag(name = "Payment Callback V1 API", description = "결제 콜백 API")
interface PaymentCallbackApiSpec {

    @Operation(summary = "결제 콜백 수신", description = "PG사로부터 결제 결과 콜백을 수신합니다.")
    fun handleCallback(@Valid request: PaymentDto.CallbackRequest): ApiResponse<Unit>
}
