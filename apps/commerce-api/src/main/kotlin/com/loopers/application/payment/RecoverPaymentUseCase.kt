package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RecoverPaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgClient: PgClient,
) {
    // TODO: 병렬 처리 전환
    @Transactional
    fun recoverAll(): Int {
        val targetStatuses = listOf(PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT)
        val payments = paymentRepository.findByStatusIn(targetStatuses)
        var recoveredCount = 0
        for (payment in payments) {
            if (recoverSingle(payment)) recoveredCount++
        }
        return recoveredCount
    }

    @Transactional
    fun recoverByOrderId(orderId: Long): Boolean {
        val payment = paymentRepository.findByOrderIdForUpdate(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        return recoverSingle(payment)
    }

    private fun recoverSingle(payment: Payment): Boolean {
        val detail = pgClient.getTransactionByOrderId(payment.orderId) ?: return false

        // 비관적 락으로 재조회하여 동시성 보호 (콜백과 동시 처리 방지)
        val lockedPayment = paymentRepository.findByOrderIdForUpdate(payment.orderId) ?: return false
        if (lockedPayment.status != PaymentStatus.REQUESTED && lockedPayment.status != PaymentStatus.TIMEOUT) {
            return false // 이미 다른 프로세스에서 처리됨
        }

        when (detail.status) {
            PgResultStatus.SUCCESS -> {
                lockedPayment.markSuccess(detail.transactionKey)
                paymentRepository.save(lockedPayment)
                val order = orderRepository.findById(OrderId(lockedPayment.orderId)) ?: return false
                order.markPaid()
                orderRepository.save(order)
                // TODO: 다음 주 이벤트 기반 전환 (재고/쿠폰 보상 트랜잭션)
                return true
            }
            PgResultStatus.FAILED -> {
                lockedPayment.markFailed(detail.reason ?: "PG 결제 실패")
                paymentRepository.save(lockedPayment)
                val order = orderRepository.findById(OrderId(lockedPayment.orderId)) ?: return false
                order.markFailed()
                orderRepository.save(order)
                return true
            }
            PgResultStatus.TIMEOUT -> return false
        }
    }
}
