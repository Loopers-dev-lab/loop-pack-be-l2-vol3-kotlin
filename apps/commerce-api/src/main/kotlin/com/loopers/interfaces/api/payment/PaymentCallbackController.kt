package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * PG 시스템에서 결제 결과를 콜백으로 전달받는 엔드포인트.
 * 인증 없이 PG에서 직접 호출하므로 별도 컨트롤러로 분리.
 */
@RestController
@RequestMapping("/api/v1/payments/callback")
class PaymentCallbackController(
    private val paymentFacade: PaymentFacade,
) {

    @PostMapping
    fun handleCallback(
        @RequestBody request: PaymentV1Dto.PaymentCallbackRequest,
    ): ApiResponse<Any> {
        paymentFacade.handleCallback(request.toCriteria())
        return ApiResponse.success()
    }
}
