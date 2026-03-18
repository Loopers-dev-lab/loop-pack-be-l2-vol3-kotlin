package com.loopers.interfaces.api.v1.payment

import com.loopers.application.payment.GetPaymentUseCase
import com.loopers.application.payment.RecoverPaymentUseCase
import com.loopers.application.payment.RequestPaymentUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController(
    private val requestPaymentUseCase: RequestPaymentUseCase,
    private val getPaymentUseCase: GetPaymentUseCase,
    private val recoverPaymentUseCase: RecoverPaymentUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun requestPayment(
        @AuthenticatedUser authUser: AuthUser,
        @Valid @RequestBody request: RequestPaymentRequest,
    ): ApiResponse<RequestPaymentResponse> {
        val paymentId = requestPaymentUseCase.request(authUser.id, request.toCommand())
        return ApiResponse.success(RequestPaymentResponse(paymentId))
    }

    @GetMapping("/{id}")
    fun getPayment(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable id: Long,
    ): ApiResponse<GetPaymentResponse> {
        val paymentInfo = getPaymentUseCase.getById(authUser.id, id)
        return ApiResponse.success(GetPaymentResponse.from(paymentInfo))
    }

    @PostMapping("/{id}/recover")
    fun recoverPayment(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable id: Long,
    ): ApiResponse<GetPaymentResponse> {
        val paymentInfo = recoverPaymentUseCase.recoverSinglePayment(id)
        return ApiResponse.success(GetPaymentResponse.from(paymentInfo))
    }
}
