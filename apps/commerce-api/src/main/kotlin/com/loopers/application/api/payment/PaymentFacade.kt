package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.dto.PaymentInfo
import com.loopers.domain.payment.event.PaymentCompleted
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentFacade(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(isolation = Isolation.SERIALIZABLE)
    fun completePayment(command: PaymentCallbackCommand) {
        log.info(
            "Processing payment callback: transactionId={}, orderId={}, amount={}, status={}",
            command.transactionId,
            command.orderId,
            command.amount,
            command.status,
        )

        try {
            // (1) 결제 기록 조회
            val payment = paymentService.getPaymentByTransactionId(command.transactionId)

            // ✅ 멱등성: 이미 완료/실패/취소된 경우 무시
            if (payment.status == PaymentStatus.COMPLETED) {
                log.warn("Payment already completed: transactionId={}", command.transactionId)
                return
            }

            // (2) PG 콜백 status에 따라 분기 처리
            when (command.status?.uppercase()) {
                "FAILED" -> {
                    payment.markAsFailed()
                    paymentService.save(payment)
                    log.info("Payment marked as failed: orderId={}, transactionId={}", command.orderId, command.transactionId)
                    return
                }
                "CANCELLED" -> {
                    payment.markAsCancelled()
                    paymentService.save(payment)
                    log.info("Payment marked as cancelled: orderId={}, transactionId={}", command.orderId, command.transactionId)
                    return
                }
                "COMPLETED" -> {
                    // 계속 진행
                }
                else -> {
                    throw CoreException(
                        ErrorType.BAD_REQUEST,
                        "알 수 없는 결제 상태: ${command.status}",
                    )
                }
            }

            // (3) 결제 완료 처리
            payment.markAsCompleted(command.amount)
            paymentService.save(payment)

            // (4) 주문 상태 변경
            val order = orderService.getOrderByIdForAdmin(command.orderId)
            order.changeStatus(OrderStatus.PAID)

            // (5) 도메인 이벤트 발행 (배송 준비 등 후처리용)
            applicationEventPublisher.publishEvent(
                PaymentCompleted(
                    source = this,
                    orderId = command.orderId,
                ),
            )

            log.info("Payment completed successfully: orderId={}, transactionId={}", command.orderId, command.transactionId)
        } catch (e: Exception) {
            log.error(
                "Payment callback processing failed: transactionId={}, orderId={}",
                command.transactionId,
                command.orderId,
                e,
            )
            throw e
        }
    }

    fun getPaymentByOrderId(orderId: Long): PaymentInfo? =
        paymentService.getPaymentByOrderId(orderId)?.let { PaymentInfo.from(it) }

    @Transactional
    fun createPayment(userId: Long, orderId: Long, cardType: String, cardNo: String): PaymentInfo {
        log.info("Creating payment: userId={}, orderId={}, cardType={}", userId, orderId, cardType)

        // (1) 주문 존재 확인 & 소유권 검증
        val order = orderService.getOrderById(userId, orderId)

        // (2) 주문 상태 확인 (PENDING만 결제 가능)
        if (order.status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문이 대기 상태가 아닙니다. 현재 상태: ${order.status}")
        }

        // (3) 이미 결제가 존재하는지 확인
        val existingPayment = paymentService.getPaymentByOrderId(orderId)
        if (existingPayment != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 이 주문에 대한 결제가 존재합니다")
        }

        // (4) 결제 생성
        val transactionId = generateTransactionId(orderId)
        val payment = paymentService.createPayment(
            orderId = orderId,
            transactionId = transactionId,
            amount = order.getTotalPrice(),
            cardType = cardType,
            cardNo = cardNo,
        )

        log.info("Payment created: paymentId={}, orderId={}, amount={}", payment.id, orderId, order.getTotalPrice())

        return PaymentInfo.from(payment)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
