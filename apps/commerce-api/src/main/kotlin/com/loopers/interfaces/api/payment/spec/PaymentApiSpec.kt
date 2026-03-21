package com.loopers.interfaces.api.payment.spec

import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid

@Tag(name = "Payment V1 API", description = "결제 API")
interface PaymentApiSpec {

    @Operation(summary = "결제 요청", description = "주문에 대한 결제를 요청합니다.")
    fun requestPayment(
        @Parameter(hidden = true) @AuthUser userId: Long,
        @Valid request: PaymentDto.PaymentRequest,
    ): ApiResponse<PaymentDto.PaymentResponse>
}
