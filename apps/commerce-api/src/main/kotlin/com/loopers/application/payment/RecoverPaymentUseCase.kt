package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RecoverPaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgClient: PgClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(orderId: Long): Boolean {
        val payment = paymentRepository.findByOrderIdForUpdate(orderId) ?: return false
        if (payment.status != PaymentStatus.REQUESTED && payment.status != PaymentStatus.TIMEOUT) return false

        val detail = pgClient.getTransactionByOrderId(orderId)
        if (detail == null) {
            log.info("PG 트랜잭션 미확인. 다음 복구 주기에 재시도. orderId={}", orderId)
            return false
        }

        val order = orderRepository.findByIdForUpdate(OrderId(orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        when (detail.status) {
            PgResultStatus.SUCCESS -> {
                payment.markSuccess(detail.transactionKey)
                paymentRepository.save(payment)
                order.markPaid()
                orderRepository.save(order)
                return true
            }
            PgResultStatus.FAILED -> {
                payment.markFailed(detail.reason ?: "PG 결제 실패")
                paymentRepository.save(payment)
                order.markFailed()
                orderRepository.save(order)
                return true
            }
            PgResultStatus.TIMEOUT -> return false
        }
    }
}
