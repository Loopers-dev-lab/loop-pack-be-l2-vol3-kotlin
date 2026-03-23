package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/payments")
class PaymentAdminV1Controller(
    private val paymentFacade: PaymentFacade,
) : PaymentAdminV1ApiSpec {

    @PostMapping("/{paymentId}/sync")
    override fun syncPayment(
        @PathVariable paymentId: Long,
    ): ApiResponse<PaymentAdminV1Dto.PaymentAdminResponse> {
        return paymentFacade.syncPayment(paymentId)
            .let { PaymentAdminV1Dto.PaymentAdminResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
