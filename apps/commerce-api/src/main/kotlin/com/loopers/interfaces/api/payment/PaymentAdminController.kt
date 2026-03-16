package com.loopers.interfaces.api.payment

import com.loopers.application.payment.RecoverAllPaymentsUseCase
import com.loopers.application.payment.RecoverPaymentUseCase
import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.api.payment.spec.PaymentAdminApiSpec
import com.loopers.interfaces.support.ApiResponse
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
    private val recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase,
) : PaymentAdminApiSpec {

    @PostMapping("/{orderId}/recover")
    override fun recoverPayment(@PathVariable orderId: Long): ApiResponse<PaymentDto.SingleRecoverResponse> {
        val recovered = recoverPaymentUseCase.execute(orderId)
        return ApiResponse.success(PaymentDto.SingleRecoverResponse(recovered))
    }

    @PostMapping("/recover-all")
    override fun recoverAllPayments(): ApiResponse<PaymentDto.RecoverResponse> {
        val count = recoverAllPaymentsUseCase.execute()
        return ApiResponse.success(PaymentDto.RecoverResponse(count))
    }
}
