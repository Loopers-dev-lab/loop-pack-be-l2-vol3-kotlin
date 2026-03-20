package com.loopers.interfaces.api.user.payment

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.payment.PaymentCreateResult
import com.loopers.application.user.payment.PaymentCreateUseCase
import com.loopers.application.user.payment.PaymentDetailUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/v1/payments")
@RestController
class UserPaymentV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val paymentCreateUseCase: PaymentCreateUseCase,
    private val paymentDetailUseCase: PaymentDetailUseCase,
) : UserPaymentV1ApiSpec {

    @PostMapping
    override fun create(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestHeader("X-Payment-Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: UserPaymentV1Request.Create,
    ): ResponseEntity<ApiResponse<UserPaymentV1Response.Created>> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        val createResult = paymentCreateUseCase.create(request.toCommand(userId, idempotencyKey))

        val response = UserPaymentV1Response.Created.from(createResult.result)
        val status = when (createResult) {
            is PaymentCreateResult.NewlyCreated -> HttpStatus.CREATED
            is PaymentCreateResult.IdempotentReplay -> HttpStatus.OK
        }

        return ResponseEntity.status(status).body(ApiResponse.success(response))
    }

    @GetMapping("/{paymentId}")
    override fun detail(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable paymentId: Long,
    ): ResponseEntity<ApiResponse<UserPaymentV1Response.Detail>> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        val result = paymentDetailUseCase.detail(paymentId, userId)
        val response = UserPaymentV1Response.Detail.from(result)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
