package com.loopers.interfaces.api.payment

import com.loopers.application.payment.HandlePaymentCallbackUseCase
import com.loopers.application.payment.PaymentCommand
import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.api.payment.spec.PaymentCallbackApiSpec
import com.loopers.interfaces.support.ApiResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/payments")
class PaymentCallbackController(
    private val handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase,
) : PaymentCallbackApiSpec {

    @PostMapping("/callback")
    override fun handleCallback(@RequestBody request: PaymentDto.CallbackRequest): ApiResponse<Unit> {
        val command = PaymentCommand.HandleCallback(
            orderId = request.orderId,
            transactionKey = request.transactionKey,
            success = request.status == PaymentDto.CallbackStatus.SUCCESS,
            reason = request.reason,
        )
        handlePaymentCallbackUseCase.execute(command)
        return ApiResponse.success(Unit)
    }
}
