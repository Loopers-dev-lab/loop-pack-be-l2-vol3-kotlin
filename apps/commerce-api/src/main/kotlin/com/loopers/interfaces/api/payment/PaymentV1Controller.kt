package com.loopers.interfaces.api.payment

import com.loopers.application.order.OrderService
import com.loopers.application.payment.PaymentCommand
import com.loopers.application.payment.PaymentFacade
import com.loopers.application.payment.PaymentInfo
import com.loopers.application.payment.PaymentService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticated
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val paymentFacade: PaymentFacade,
    private val paymentService: PaymentService,
    private val orderService: OrderService,
) : PaymentV1ApiSpec {

    @MemberAuthenticated
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun requestPayment(
        authenticatedMember: AuthenticatedMember,
        @RequestBody @Valid request: PaymentV1Dto.PaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val command = PaymentCommand.RequestPayment(
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
            amount = request.amount,
        )
        val paymentInfo = paymentFacade.requestPayment(authenticatedMember.id, command)

        val order = orderService.getOrderById(request.orderId)
        val pgResult = paymentFacade.callPg(paymentInfo.id, authenticatedMember.id, order.orderNumber)

        return PaymentV1Dto.PaymentResponse.from(pgResult)
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/callback")
    override fun callback(
        @RequestBody request: PaymentV1Dto.CallbackRequest,
    ): ApiResponse<Any> {
        paymentFacade.handleCallback(request.transactionKey, request.status, request.reason)
        return ApiResponse.success()
    }

    @MemberAuthenticated
    @GetMapping
    override fun getPayment(
        authenticatedMember: AuthenticatedMember,
        @RequestParam orderId: Long,
    ): ApiResponse<List<PaymentV1Dto.PaymentResponse>> {
        val payments = paymentService.getPaymentsByOrderId(orderId)
        return payments.map { PaymentInfo.from(it) }
            .map { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
