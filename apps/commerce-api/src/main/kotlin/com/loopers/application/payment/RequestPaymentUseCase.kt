package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class RequestPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentPgProcessor: PaymentPgProcessor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(command: PaymentCommand.RequestPayment): PaymentInfo {
        // 1. Order 조회 (비관적 락) + 소유자 검증
        val order = orderRepository.findByIdForUpdate(OrderId(command.orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        if (order.refUserId != UserId(command.userId)) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }

        // 2. 중복 결제 방지
        paymentRepository.findByOrderIdForUpdate(command.orderId)?.let { existingPayment ->
            when (existingPayment.status) {
                PaymentStatus.SUCCESS ->
                    throw CoreException(ErrorType.CONFLICT, "이미 결제가 완료된 주문입니다.")
                PaymentStatus.REQUESTED, PaymentStatus.TIMEOUT ->
                    throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중인 주문입니다.")
                PaymentStatus.FAILED -> { /* 재결제 허용 */ }
            }
        }

        // 3. Order → PENDING_PAYMENT
        order.markPendingPayment()
        orderRepository.save(order)

        // 4. Payment(REQUESTED) 먼저 저장
        val cardType = try {
            CardType.valueOf(command.cardType)
        } catch (e: IllegalArgumentException) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 카드 유형입니다: ${command.cardType}")
        }
        val payment = Payment.create(
            orderId = command.orderId,
            cardType = cardType,
            cardNo = command.cardNo,
            amount = order.totalPrice.value.setScale(0, java.math.RoundingMode.UNNECESSARY).toLong(),
        )
        val savedPayment = paymentRepository.save(payment)

        // 5. 커밋 후 PG 호출 (트랜잭션 밖에서 외부 HTTP 호출)
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        try {
                            paymentPgProcessor.processPayment(
                                paymentId = savedPayment.id,
                                orderId = command.orderId,
                                amount = savedPayment.amount,
                                cardType = command.cardType,
                                cardNo = command.cardNo,
                            )
                        } catch (e: Exception) {
                            log.error(
                                "PG 결제 요청 실패. paymentId={}, orderId={}: {}",
                                savedPayment.id,
                                command.orderId,
                                e.message,
                                e,
                            )
                        }
                    }
                },
            )
        } else {
            paymentPgProcessor.processPayment(
                paymentId = savedPayment.id,
                orderId = command.orderId,
                amount = savedPayment.amount,
                cardType = command.cardType,
                cardNo = command.cardNo,
            )
        }

        return PaymentInfo.from(savedPayment)
    }
}
