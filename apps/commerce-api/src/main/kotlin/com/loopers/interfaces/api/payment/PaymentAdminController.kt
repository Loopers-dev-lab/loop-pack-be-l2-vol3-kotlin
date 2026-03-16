package com.loopers.interfaces.api.payment

import com.loopers.application.payment.RecoverPaymentUseCase
import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.support.ApiResponse
import jakarta.validation.constraints.Positive
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api-admin/v1/payments")
class PaymentAdminController(
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
) {

    @PostMapping("/{orderId}/recover")
    fun recoverPayment(@PathVariable @Positive orderId: Long): ApiResponse<PaymentDto.SingleRecoverResponse> {
        val recovered = recoverPaymentUseCase.recoverByOrderId(orderId)
        return ApiResponse.success(PaymentDto.SingleRecoverResponse(recovered))
    }

    @PostMapping("/recover-all")
    fun recoverAllPayments(): ApiResponse<PaymentDto.RecoverResponse> {
        val count = recoverPaymentUseCase.recoverAll()
        return ApiResponse.success(PaymentDto.RecoverResponse(count))
    }
}
