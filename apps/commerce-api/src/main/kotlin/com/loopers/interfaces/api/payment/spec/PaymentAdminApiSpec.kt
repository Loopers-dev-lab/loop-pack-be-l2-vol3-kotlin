package com.loopers.interfaces.api.payment.spec

import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.support.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Positive

@Tag(name = "Payment Admin V1 API", description = "결제 관리 API")
interface PaymentAdminApiSpec {

    @Operation(summary = "단건 결제 복구", description = "특정 주문의 결제를 복구합니다.")
    fun recoverPayment(@Positive orderId: Long): ApiResponse<PaymentDto.SingleRecoverResponse>

    @Operation(summary = "전체 결제 복구", description = "복구 대상 결제를 일괄 복구합니다.")
    fun recoverAllPayments(): ApiResponse<PaymentDto.RecoverResponse>
}
