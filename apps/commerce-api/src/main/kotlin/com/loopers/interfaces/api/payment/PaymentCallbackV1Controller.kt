package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCallbackCriteria
import com.loopers.application.payment.PaymentCallbackUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/v1/payments")
class PaymentCallbackV1Controller(
    private val paymentCallbackUseCase: PaymentCallbackUseCase,
) {
    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    fun handleCallback(
        @RequestBody request: PaymentV1Dto.CallbackRequest,
    ): ApiResponse<Any> {
        val criteria = PaymentCallbackCriteria(
            transactionKey = request.transactionKey,
            status = request.status,
            reason = request.reason,
        )
        paymentCallbackUseCase.execute(criteria)
        return ApiResponse.success()
    }
}
