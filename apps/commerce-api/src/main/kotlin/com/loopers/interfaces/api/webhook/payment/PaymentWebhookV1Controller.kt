package com.loopers.interfaces.api.webhook.payment

import com.loopers.application.user.payment.PaymentCallbackUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/webhook/v1/payments")
@RestController
class PaymentWebhookV1Controller(
    private val paymentCallbackUseCase: PaymentCallbackUseCase,
) {

    @PostMapping("/{paymentId}")
    fun handleCallback(
        @PathVariable paymentId: Long,
        @RequestBody request: PaymentWebhookV1Request.Callback,
    ): ResponseEntity<Void> {
        paymentCallbackUseCase.handleCallback(request.toCommand(paymentId))
        return ResponseEntity.ok().build()
    }
}
