package com.loopers.interfaces.api.payment

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.payment.PaymentUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val authUseCase: AuthUseCase,
    private val paymentUseCase: PaymentUseCase,
) : PaymentV1ApiSpec {

    @PostMapping
    override fun requestPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @Valid @RequestBody request: PaymentV1Dto.Request,
    ): ApiResponse<PaymentV1Dto.DetailResponse> {
        val member = authUseCase.authenticate(loginId, password)
        return paymentUseCase.requestPayment(member.id!!, request.toCommand())
            .let(PaymentV1Dto.DetailResponse::from)
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/{orderId}/sync")
    override fun syncPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable orderId: Long,
    ): ApiResponse<PaymentV1Dto.DetailResponse> {
        val member = authUseCase.authenticate(loginId, password)
        return paymentUseCase.syncPayment(member.id!!, orderId)
            .let(PaymentV1Dto.DetailResponse::from)
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/callback")
    fun handleCallback(
        @RequestBody request: PaymentV1Dto.CallbackRequest,
    ): ApiResponse<Any> {
        paymentUseCase.handleCallback(request.toCommand())
        return ApiResponse.success()
    }
}
