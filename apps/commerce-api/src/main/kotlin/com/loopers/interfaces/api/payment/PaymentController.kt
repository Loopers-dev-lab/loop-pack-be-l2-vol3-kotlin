package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCommand
import com.loopers.application.payment.RequestPaymentUseCase
import com.loopers.interfaces.api.payment.dto.PaymentDto
import com.loopers.interfaces.api.payment.spec.PaymentApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val requestPaymentUseCase: RequestPaymentUseCase,
) : PaymentApiSpec {

    @PostMapping
    override fun requestPayment(
        @AuthUser userId: Long,
        @RequestBody request: PaymentDto.PaymentRequest,
    ): ApiResponse<PaymentDto.PaymentResponse> {
        val command = PaymentCommand.RequestPayment(
            userId = userId,
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
        )
        return requestPaymentUseCase.execute(command)
            .let { PaymentDto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
