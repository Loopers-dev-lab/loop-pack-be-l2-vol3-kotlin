package com.loopers.interfaces.api.v1.payment

import com.loopers.application.payment.HandlePaymentCallbackUseCase
import com.loopers.application.payment.PaymentCallbackInfo
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments/callback")
class PaymentCallbackController(
    private val handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase,
) {

    @PostMapping
    fun handleCallback(
        @RequestBody callbackInfo: PaymentCallbackInfo,
    ): ApiResponse<Nothing?> {
        handlePaymentCallbackUseCase.handle(callbackInfo)
        return ApiResponse.success(null)
    }
}
