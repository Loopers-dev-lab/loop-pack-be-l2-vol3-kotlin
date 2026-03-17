package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.dto.PaymentInfo
import com.loopers.domain.payment.event.PaymentCompleted
import com.loopers.infrastructure.payment.pg.PgPaymentGateway
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
    private val pgPaymentGateway: PgPaymentGateway,
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
            // (1) 결제 기록 조회 (행 락 취득 - 동시 요청 시 대기)
            val payment = paymentService.getPaymentByTransactionIdForUpdate(command.transactionId)

            // ✅ 멱등성: INITIATED 상태가 아니면 무시 (어떤 콜백이 와도 이미 처리됨)
            if (payment.status != PaymentStatus.INITIATED) {
                log.warn(
                    "Payment is not in INITIATED state: status={}, transactionId={}",
                    payment.status,
                    command.transactionId,
                )
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
            orderService.saveOrder(order)

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
    fun createPayment(
        userId: Long,
        orderId: Long,
        cardType: String,
        cardNo: String,
    ): PaymentInfo {
        val callbackUrl = "http://localhost:8080/api/v1/payments/callback"
        log.info("Creating payment: userId={}, orderId={}, cardType={}", userId, orderId, cardType)

        // (1) PENDING 상태의 주문을 행 락으로 조회 (동시 결제 요청 시 대기)
        // - 쿼리 단계에서 상태 조건 확인으로 명확성과 안전성 향상
        val order = orderService.getOrderByIdForUpdateWithPending(userId, orderId)

        // (2) 이미 결제가 존재하는지 확인 (락 상태에서 확인하므로 race condition 방지)
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

        // (5) PG로 결제 요청
        try {
            val pgResult = pgPaymentGateway.requestPayment(
                userId = userId,
                transactionId = transactionId,
                orderId = orderId,
                amount = order.getTotalPrice(),
                cardType = cardType,
                cardNo = cardNo,
                callbackUrl = callbackUrl,
            )
            log.info("PG payment request successful: requestId={}, status={}", pgResult.requestId, pgResult.status)
        } catch (e: Exception) {
            log.error("PG payment request failed: transactionId={}, orderId={}", transactionId, orderId, e)
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 결제 요청에 실패했습니다")
        }

        log.info("Payment created: paymentId={}, orderId={}, amount={}", payment.id, orderId, order.getTotalPrice())

        return PaymentInfo.from(payment)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
