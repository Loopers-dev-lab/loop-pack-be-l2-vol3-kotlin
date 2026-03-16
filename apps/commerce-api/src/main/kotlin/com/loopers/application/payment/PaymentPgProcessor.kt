package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface PaymentPgProcessor {
    fun processPayment(paymentId: Long, orderId: Long, amount: Long, cardType: String, cardNo: String)
}

@Component
class PaymentPgProcessorImpl(
    private val pgClient: PgClient,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
) : PaymentPgProcessor {
    @Transactional
    override fun processPayment(paymentId: Long, orderId: Long, amount: Long, cardType: String, cardNo: String) {
        val pgResult = pgClient.requestPayment(
            PgPaymentRequest(
                orderId = orderId,
                cardType = CardType.valueOf(cardType),
                cardNo = cardNo,
                amount = amount,
            ),
        )

        when (pgResult.status) {
            PgResultStatus.SUCCESS -> {
                // REQUESTED 상태 유지 (콜백 대기)
            }
            PgResultStatus.TIMEOUT -> {
                val timeoutPayment = paymentRepository.findByOrderIdForUpdate(orderId)
                    ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다. orderId=$orderId")
                timeoutPayment.markTimeout()
                paymentRepository.save(timeoutPayment)
            }
            PgResultStatus.FAILED -> {
                val order = orderRepository.findById(OrderId(orderId))
                    ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다. orderId=$orderId")
                val failedPayment = paymentRepository.findByOrderIdForUpdate(orderId)
                    ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다. orderId=$orderId")
                failedPayment.markFailed(pgResult.reason ?: "PG 결제 실패")
                paymentRepository.save(failedPayment)
                order.markFailed()
                orderRepository.save(order)
            }
        }
    }
}
