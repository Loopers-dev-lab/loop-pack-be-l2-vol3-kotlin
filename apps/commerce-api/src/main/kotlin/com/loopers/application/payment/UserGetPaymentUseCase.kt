package com.loopers.application.payment

import com.loopers.application.UseCase
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class UserGetPaymentUseCase(
    private val userService: UserService,
    private val paymentService: PaymentService,
) : UseCase<GetPaymentCriteria, GetPaymentResult> {

    override fun execute(criteria: GetPaymentCriteria): GetPaymentResult {
        val user = userService.getUser(criteria.loginId)
        val payment = paymentService.getPayment(criteria.paymentId)

        if (payment.userId != user.id) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 결제만 조회할 수 있습니다.")
        }

        return GetPaymentResult.from(payment)
    }
}

data class GetPaymentCriteria(
    val loginId: String,
    val paymentId: Long,
) {
    init {
        if (paymentId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 ID는 0보다 커야 합니다.")
        }
    }
}
