package com.loopers.application.payment.support

import com.loopers.domain.coupon.IssuedCouponProcessor
import com.loopers.domain.payment.PaymentStatus
import org.springframework.stereotype.Component

@Component
class PaymentCouponStateSyncer(
    private val issuedCouponProcessor: IssuedCouponProcessor,
) {
    fun sync(issuedCouponId: Long?, paymentStatus: PaymentStatus) {
        if (issuedCouponId == null) {
            return
        }

        when (paymentStatus) {
            PaymentStatus.SUCCESS -> issuedCouponProcessor.confirmUseIfReserved(issuedCouponId)
            PaymentStatus.REQUEST_FAILED,
            PaymentStatus.FAILED,
            -> issuedCouponProcessor.releaseIfReserved(issuedCouponId)

            PaymentStatus.REQUESTED,
            PaymentStatus.PENDING,
            PaymentStatus.UNKNOWN,
            -> Unit
        }
    }
}
