package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val paymentFacade: PaymentFacade,
) : PaymentV1ApiSpec {

    @PostMapping
    override fun requestPayment(
        @CurrentUser loginUser: LoginUser,
        @RequestBody request: PaymentV1Dto.RequestPaymentRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        return paymentFacade.requestPayment(loginUser.id, request.toCriteria())
            .let { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{paymentId}")
    override fun getPayment(
        @CurrentUser loginUser: LoginUser,
        @PathVariable paymentId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        return paymentFacade.getPayment(loginUser.id, paymentId)
            .let { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getPaymentsByOrderId(
        @CurrentUser loginUser: LoginUser,
        @RequestParam orderId: Long,
    ): ApiResponse<List<PaymentV1Dto.PaymentResponse>> {
        return paymentFacade.getPaymentsByOrderId(loginUser.id, orderId)
            .map { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
