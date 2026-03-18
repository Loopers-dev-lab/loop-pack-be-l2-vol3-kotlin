package com.loopers.application.api.payment

import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.ReceiptService
import com.loopers.domain.payment.dto.ReceiptInfo
import com.loopers.infrastructure.payment.pg.PgPaymentGateway
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentFacade(
    private val receiptService: ReceiptService,
    private val orderService: OrderService,
    private val pgPaymentGateway: PgPaymentGateway,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun completePayment(command: PaymentCallbackCommand) {
        // (1) 결제 상태 업데이트
        receiptService.updateReceiptStatus(command)

        // (2) 주문 상태 변경
        orderService.markOrderAsPaid(command.orderId)

        log.info("Payment completed successfully: orderId={}, transactionId={}", command.orderId, command.transactionId)
    }

    @Transactional
    fun createPayment(
        userId: Long,
        orderId: Long,
        cardType: String,
        cardNo: String,
    ): ReceiptInfo {
        val callbackUrl = "http://localhost:8080/api/v1/payments/callback"
        log.info("Creating payment: userId={}, orderId={}, cardType={}", userId, orderId, cardType)

        // (1) PENDING 상태의 주문을 행 락으로 조회 (동시 결제 요청 시 대기)
        // - 쿼리 단계에서 상태 조건 확인으로 명확성과 안전성 향상
        val order = orderService.getOrderByIdForUpdateWithPending(userId, orderId)

        // (2) 이미 결제가 존재하는지 확인 (락 상태에서 확인하므로 race condition 방지)
        val existingReceipt = receiptService.getReceiptByOrderId(orderId)
        if (existingReceipt != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 이 주문에 대한 결제가 존재합니다")
        }

        // (4) 결제 시작
        val transactionId = generateTransactionId(orderId)
        val receipt = receiptService.initiateReceipt(
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

        log.info("Payment created: paymentId={}, orderId={}, amount={}", receipt.id, orderId, order.getTotalPrice())

        return ReceiptInfo.from(receipt)
    }

    private fun generateTransactionId(orderId: Long): String {
        return "TXN_${System.currentTimeMillis()}_$orderId"
    }
}
