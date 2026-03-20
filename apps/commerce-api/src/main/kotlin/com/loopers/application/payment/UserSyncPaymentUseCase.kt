package com.loopers.application.payment

import com.loopers.application.UseCase
import com.loopers.domain.payment.CompletePaymentCommand
import com.loopers.domain.payment.FailPaymentCommand
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgClient
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UserSyncPaymentUseCase(
    private val userService: UserService,
    private val paymentService: PaymentService,
    private val pgClient: PgClient,
) : UseCase<SyncPaymentCriteria, SyncPaymentResult> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(criteria: SyncPaymentCriteria): SyncPaymentResult {
        val user = userService.getUser(criteria.loginId)
        val payment = paymentService.getPayment(criteria.paymentId)

        if (payment.userId != user.id) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 결제만 조회할 수 있습니다.")
        }

        if (payment.status != PaymentStatus.PENDING) {
            return SyncPaymentResult.from(payment)
        }

        val transactionKey = payment.transactionKey
            ?: return SyncPaymentResult.from(payment)

        return try {
            val pgStatus = pgClient.getPaymentStatus(user.id, transactionKey)
            val updatedPayment = when (pgStatus.status.uppercase()) {
                "SUCCESS" -> paymentService.completePayment(CompletePaymentCommand(transactionKey = transactionKey))
                "FAILED" -> paymentService.failPayment(
                    FailPaymentCommand(transactionKey = transactionKey, reason = pgStatus.reason),
                )
                else -> payment
            }
            SyncPaymentResult.from(updatedPayment)
        } catch (e: CoreException) {
            log.warn("PG 상태 동기화 실패: paymentId={}, error={}", criteria.paymentId, e.message)
            SyncPaymentResult.from(payment)
        }
    }
}
