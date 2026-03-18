package com.loopers.interfaces.api.payment

import com.loopers.application.api.payment.PaymentFacade
import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.math.RoundingMode

@RestController
@RequestMapping("/api/v1/payments")
class PaymentCallbackController(
    private val paymentFacade: PaymentFacade,
) : PaymentCallbackApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun requestPayment(
        @RequestAttribute("userId") userId: Long,
        @RequestBody @Valid request: PaymentV1Dto.CreatePaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val paymentInfo = paymentFacade.requestPayment(
            userId = userId,
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
        )

        return ApiResponse.success(
            PaymentV1Dto.PaymentResponse(
                paymentId = paymentInfo.id,
                orderId = paymentInfo.orderId,
                amount = paymentInfo.amount.toString(),
                status = paymentInfo.status.name,
                cardType = request.cardType,
                cardNo = request.cardNo,
            ),
        )
    }

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.OK)
    override fun handleCallback(
        @RequestBody @Valid callbackData: PaymentCallbackDto.CallbackRequest,
    ): ApiResponse<Unit> {
        // (1) 결제 처리
        paymentFacade.completePayment(
            PaymentCallbackCommand(
                transactionId = callbackData.transactionKey,
                orderId = callbackData.orderId.toLong(),
                amount = BigDecimal(callbackData.amount).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP),
                status = callbackData.status.name,
                reason = callbackData.reason,
            ),
        )

        return ApiResponse.success(Unit)
    }
}
