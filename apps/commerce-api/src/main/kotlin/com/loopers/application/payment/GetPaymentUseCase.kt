package com.loopers.application.payment

import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPaymentUseCase(
    private val paymentRepository: PaymentRepository,
) {

    fun getById(userId: Long, paymentId: Long): PaymentInfo {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다: $paymentId")

        if (payment.refUserId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "타인의 결제 정보입니다.")
        }

        return PaymentInfo.from(payment)
    }
}
