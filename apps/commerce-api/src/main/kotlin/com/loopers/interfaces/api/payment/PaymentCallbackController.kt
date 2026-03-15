package com.loopers.interfaces.api.payment

import com.loopers.application.api.payment.PaymentFacade
import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.infrastructure.payment.pg.PgPaymentGateway
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentCallbackController(
    private val paymentFacade: PaymentFacade,
    private val pgPaymentGateway: PgPaymentGateway,
) : PaymentCallbackApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createPayment(
        @RequestAttribute("userId") userId: Long,
        @RequestBody @Valid request: PaymentV1Dto.CreatePaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val paymentInfo = paymentFacade.createPayment(
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
        // (1) PG 서명 검증
        val isValid = pgPaymentGateway.verifySignature(
            transactionId = callbackData.transactionId,
            amount = callbackData.amount,
            signature = callbackData.signature,
        )
        if (!isValid) {
            throw CoreException(ErrorType.BAD_REQUEST, "PG 서명이 유효하지 않습니다")
        }

        // (2) 결제 완료 처리
        paymentFacade.completePayment(
            PaymentCallbackCommand(
                transactionId = callbackData.transactionId,
                orderId = callbackData.orderId,
                amount = callbackData.amount,
                status = callbackData.status,
            ),
        )

        return ApiResponse.success(Unit)
    }
}
