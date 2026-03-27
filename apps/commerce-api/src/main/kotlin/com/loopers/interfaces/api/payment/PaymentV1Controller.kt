package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.infrastructure.payment.PgCallbackSignatureVerifier
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val paymentFacade: PaymentFacade,
    private val pgCallbackSignatureVerifier: PgCallbackSignatureVerifier,
) {
    @PostMapping("/callbacks/pg")
    fun handlePgCallback(
        @RequestBody request: PaymentV1Dto.PgCallbackRequest,
    ): ApiResponse<Any> {
        val isValidSignature = pgCallbackSignatureVerifier.verify(
            payload = request.signaturePayload(),
            signature = request.signature,
        )
        if (!isValidSignature) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 PG 콜백 서명입니다.")
        }

        paymentFacade.handleCallback(request.toCommand())
        return ApiResponse.success()
    }
}
