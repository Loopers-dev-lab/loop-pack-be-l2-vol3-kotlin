package com.loopers.interfaces.api.payment

import com.loopers.application.payment.GetPaymentCriteria
import com.loopers.application.payment.RequestPaymentCriteria
import com.loopers.application.payment.SyncPaymentCriteria
import com.loopers.application.payment.UserGetPaymentUseCase
import com.loopers.application.payment.UserRequestPaymentUseCase
import com.loopers.application.payment.UserSyncPaymentUseCase
import com.loopers.interfaces.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentV1Controller(
    private val userRequestPaymentUseCase: UserRequestPaymentUseCase,
    private val userGetPaymentUseCase: UserGetPaymentUseCase,
    private val userSyncPaymentUseCase: UserSyncPaymentUseCase,
) : PaymentV1ApiSpec {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun requestPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @Valid @RequestBody request: PaymentV1Dto.RequestPaymentRequest,
    ): ApiResponse<PaymentV1Dto.RequestPaymentResponse> {
        val criteria = RequestPaymentCriteria(
            loginId = loginId,
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
        )
        return userRequestPaymentUseCase.execute(criteria)
            .let { PaymentV1Dto.RequestPaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.OK)
    override fun getPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable paymentId: Long,
    ): ApiResponse<PaymentV1Dto.PaymentResponse> {
        val criteria = GetPaymentCriteria(loginId = loginId, paymentId = paymentId)
        return userGetPaymentUseCase.execute(criteria)
            .let { PaymentV1Dto.PaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @PostMapping("/{paymentId}/sync")
    @ResponseStatus(HttpStatus.OK)
    override fun syncPayment(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable paymentId: Long,
    ): ApiResponse<PaymentV1Dto.SyncPaymentResponse> {
        val criteria = SyncPaymentCriteria(loginId = loginId, paymentId = paymentId)
        return userSyncPaymentUseCase.execute(criteria)
            .let { PaymentV1Dto.SyncPaymentResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
