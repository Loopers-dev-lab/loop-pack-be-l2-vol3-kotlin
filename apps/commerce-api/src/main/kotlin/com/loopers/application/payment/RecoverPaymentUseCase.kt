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
        val payment = paymentRepository.findByOrderId(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다.")
        return recoverSingle(payment)
    }

    private fun recoverSingle(payment: Payment): Boolean {
        val detail = pgClient.getTransactionByOrderId(payment.orderId) ?: return false

        when (detail.status) {
            PgResultStatus.SUCCESS -> {
                payment.markSuccess(detail.transactionKey)
                paymentRepository.save(payment)
                val order = orderRepository.findById(OrderId(payment.orderId)) ?: return false
                order.markPaid()
                orderRepository.save(order)
                // TODO: 다음 주 이벤트 기반 전환 (재고/쿠폰 보상 트랜잭션)
                return true
            }
            PgResultStatus.FAILED -> {
                payment.markFailed(detail.reason ?: "PG 결제 실패")
                paymentRepository.save(payment)
                val order = orderRepository.findById(OrderId(payment.orderId)) ?: return false
                order.markFailed()
                orderRepository.save(order)
                return true
            }
            PgResultStatus.TIMEOUT -> return false
        }
    }
}
