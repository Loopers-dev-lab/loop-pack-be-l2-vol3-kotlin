package com.loopers.application.payment

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CreatePaymentCommand
import com.loopers.domain.payment.PGPaymentClient
import com.loopers.domain.payment.PGPaymentRequest
import com.loopers.domain.payment.PaymentInfo
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.ratelimit.RateLimit
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentFacade(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val pgPaymentClient: PGPaymentClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 결제 요청.
     * 트랜잭션을 3단계로 분리:
     * 1. 주문 검증 + Payment 생성 (트랜잭션 1)
     * 2. PG 호출 (트랜잭션 밖 - 외부 API)
     * 3. transactionKey 저장 or 타임아웃 처리 (트랜잭션 2)
     */
    @RateLimit(
        key = "payment:{criteria.orderId}",
        ttl = 10,
        throwOnDuplicate = true,
        message = "이미 결제 요청이 진행 중입니다. 잠시 후 다시 시도해주세요.",
    )
    fun requestPayment(userId: Long, criteria: RequestPaymentCriteria): PaymentResult {
        // ── 트랜잭션 1: 주문 검증 + Payment 생성 ──
        val order = orderService.findById(criteria.orderId)

        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "접근 권한이 없습니다.")
        }

        if (order.orderStatus == OrderStatus.CONFIRMED) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 결제 완료된 주문입니다.")
        }

        val existingPayments = paymentService.findByOrderId(criteria.orderId)
        val hasPendingPayment = existingPayments.any {
            it.paymentStatus == PaymentStatus.REQUESTED
        }
        if (hasPendingPayment) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 결제 요청이 진행 중입니다.")
        }

        val payment = paymentService.createPayment(
            CreatePaymentCommand(
                orderId = criteria.orderId,
                userId = userId,
                amount = order.totalAmount,
                cardType = criteria.cardType,
                cardNo = criteria.cardNo,
            ),
        )

        // ── PG 호출 (트랜잭션 밖) ──
        try {
            val pgResponse = pgPaymentClient.requestPayment(
                PGPaymentRequest(
                    userId = userId,
                    orderId = order.orderNumber,
                    cardType = criteria.cardType,
                    cardNo = criteria.cardNo,
                    amount = order.totalAmount.value(),
                ),
            )

            // ── 트랜잭션 2: transactionKey 저장 ──
            paymentService.updateTransactionKey(payment.id, pgResponse.transactionKey)

            return PaymentResult.from(PaymentInfo.from(paymentService.findById(payment.id)))
        } catch (ex: CoreException) {
            // PG 호출 실패 (서킷 OPEN, 타임아웃 등)
            log.warn("PG 결제 요청 실패 - paymentId: {}, error: {}", payment.id, ex.message)
            paymentService.markTimeout(payment.id)
            return PaymentResult.from(PaymentInfo.from(paymentService.findById(payment.id)))
        }
    }

    /**
     * PG 콜백 처리.
     * PG에서 결제 결과를 콜백으로 전달받아 내부 상태를 갱신한다.
     * Payment + Order 두 도메인을 조율하므로 Facade에서 트랜잭션을 관리.
     */
    @Transactional
    fun handleCallback(criteria: PaymentCallbackCriteria) {
        log.info("PG 콜백 수신 - transactionKey: {}, status: {}, reason: {}", criteria.transactionKey, criteria.status, criteria.reason)
        val payment = paymentService.findByTransactionKey(criteria.transactionKey)

        // 멱등성: 이미 최종 상태면 무시
        if (payment.isFinalized()) {
            log.info("이미 처리된 결제 콜백 무시 - transactionKey: {}", criteria.transactionKey)
            return
        }

        val pgStatus = paymentService.syncPaymentStatus(payment.id, criteria.status, criteria.reason)

        // 결제 성공 시 Order 상태를 CONFIRMED으로 변경
        if (pgStatus == "SUCCESS") {
            orderService.markConfirmed(payment.orderId)
        }
        // 결제 실패 시 Order 상태 변경 안 함 (ORDERED 유지, 재결제 가능)

        log.info("PG 콜백 처리 완료 - paymentId: {}, pgStatus: {}", payment.id, pgStatus)
    }

    /**
     * 결제 상태 복구 (스케줄러 / Admin 수동 호출).
     * REQUESTED, TIMEOUT 상태인 결제건을 PG에 확인하여 상태를 동기화한다.
     */
    fun recoverPendingPayments() {
        val pendingPayments = paymentService.findPendingPayments()
        log.info("결제 복구 시작 - 대상 건수: {}", pendingPayments.size)

        pendingPayments.forEach { payment ->
            try {
                if (payment.transactionKey != null) {
                    val pgDetail = pgPaymentClient.getPaymentByTransactionKey(
                        payment.userId,
                        payment.transactionKey!!,
                    )
                    val pgStatus = paymentService.syncPaymentStatus(payment.id, pgDetail.status, pgDetail.reason)
                    if (pgStatus == "SUCCESS") {
                        orderService.markConfirmed(payment.orderId)
                    }
                } else {
                    val order = orderService.findById(payment.orderId)
                    val pgOrderPayments = pgPaymentClient.getPaymentsByOrderId(
                        payment.userId,
                        order.orderNumber,
                    )

                    if (pgOrderPayments.transactions.isEmpty()) {
                        log.info("PG에 기록 없는 결제건 - paymentId: {}, 만료 대상", payment.id)
                        paymentService.markRejected(payment.id, "PG 요청 미도달")
                        // Order 상태 변경 안 함 (ORDERED 유지, 재결제 가능)
                    } else {
                        val latestTransaction = pgOrderPayments.transactions.last()
                        paymentService.updateTransactionKey(payment.id, latestTransaction.transactionKey)
                        val pgStatus = paymentService.syncPaymentStatus(
                            payment.id,
                            latestTransaction.status,
                            latestTransaction.reason,
                        )
                        if (pgStatus == "SUCCESS") {
                            orderService.markConfirmed(payment.orderId)
                        }
                    }
                }
            } catch (ex: Exception) {
                log.error("결제 복구 실패 - paymentId: {}, error: {}", payment.id, ex.message)
            }
        }
    }

    /**
     * 단건 결제 상태 동기화 (Admin 수동 호출).
     */
    fun syncPayment(paymentId: Long): PaymentResult {
        val payment = paymentService.findById(paymentId)

        if (payment.transactionKey != null) {
            val pgDetail = pgPaymentClient.getPaymentByTransactionKey(
                payment.userId,
                payment.transactionKey!!,
            )
            val pgStatus = paymentService.syncPaymentStatus(payment.id, pgDetail.status, pgDetail.reason)
            if (pgStatus == "SUCCESS") {
                orderService.markConfirmed(payment.orderId)
            }
        } else {
            val order = orderService.findById(payment.orderId)
            val pgOrderPayments = pgPaymentClient.getPaymentsByOrderId(
                payment.userId,
                order.orderNumber,
            )

            if (pgOrderPayments.transactions.isEmpty()) {
                paymentService.markRejected(payment.id, "PG 요청 미도달")
            } else {
                val latestTransaction = pgOrderPayments.transactions.last()
                paymentService.updateTransactionKey(payment.id, latestTransaction.transactionKey)
                val pgStatus = paymentService.syncPaymentStatus(
                    payment.id,
                    latestTransaction.status,
                    latestTransaction.reason,
                )
                if (pgStatus == "SUCCESS") {
                    orderService.markConfirmed(payment.orderId)
                }
            }
        }

        return PaymentResult.from(PaymentInfo.from(paymentService.findById(paymentId)))
    }

    fun getPayment(userId: Long, paymentId: Long): PaymentResult {
        val payment = paymentService.findById(paymentId)
        if (payment.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "접근 권한이 없습니다.")
        }
        return PaymentResult.from(PaymentInfo.from(payment))
    }

    fun getPaymentsByOrderId(userId: Long, orderId: Long): List<PaymentResult> {
        val order = orderService.findById(orderId)
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "접근 권한이 없습니다.")
        }
        return paymentService.findByOrderId(orderId)
            .map { PaymentResult.from(PaymentInfo.from(it)) }
    }
}
