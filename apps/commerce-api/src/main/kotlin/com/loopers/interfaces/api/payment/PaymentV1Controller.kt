package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.application.user.UserService
import com.loopers.infrastructure.pg.PgCallbackRequest
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PaymentV1Controller(
    private val userService: UserService,
    private val paymentFacade: PaymentFacade,
) : PaymentV1ApiSpec {

    @PostMapping("/api/v1/payments")
    override fun requestPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: PaymentV1Dto.CreateRequest,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val user = userService.authenticate(loginId, password)
        val result = paymentFacade.requestPayment(user.id, request.toCriteria())
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(result))
    }

    @GetMapping("/api/v1/payments")
    override fun getPaymentStatus(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestParam orderId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val user = userService.authenticate(loginId, password)
        val result = paymentFacade.getPaymentStatus(user.id, orderId)
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(result))
    }

    @PostMapping("/api/v1/payments/callback")
    override fun handleCallback(
        @RequestBody request: PgCallbackRequest,
    ): ApiResponse<Any> {
        paymentFacade.handleCallback(request)
        return ApiResponse.success()
    }
}
