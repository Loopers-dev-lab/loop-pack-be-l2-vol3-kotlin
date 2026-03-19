package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.auth.AuthenticatedUserInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val paymentFacade: PaymentFacade,
) : PaymentApiSpec {

    @PostMapping
    override fun requestPayment(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @RequestBody request: PaymentDto.PaymentRequest,
    ): ApiResponse<PaymentDto.PaymentResponse> {
        val info = paymentFacade.requestPayment(
            userId = userInfo.id,
            orderId = request.orderId,
            cardType = request.toCardType(),
            cardNo = request.cardNo,
            amount = request.amount,
        )
        return ApiResponse.success(PaymentDto.PaymentResponse.from(info))
    }

    @PostMapping("/callback")
    override fun handleCallback(
        @RequestBody request: PaymentDto.CallbackRequest,
    ): ApiResponse<Unit> {
        paymentFacade.handleCallback(request.transactionKey, request.status, request.reason)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{orderId}/sync")
    override fun syncPayment(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable orderId: String,
    ): ApiResponse<List<PaymentDto.PaymentResponse>> {
        val payments = paymentFacade.syncPaymentStatus(orderId)
        return ApiResponse.success(payments.map { PaymentDto.PaymentResponse.from(it) })
    }
}
