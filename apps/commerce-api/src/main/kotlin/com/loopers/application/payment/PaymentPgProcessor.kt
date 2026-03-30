package com.loopers.application.payment

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.model.OrderOutboxEventType
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

interface PaymentPgProcessor {
    fun processPayment(paymentId: Long, orderId: Long, amount: Long, cardType: String, cardNo: String)
}

@Component
class PaymentPgProcessorImpl(
    private val pgClient: PgClient,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val txTemplate: TransactionTemplate,
    private val orderOutboxRepository: OrderOutboxRepository,
) : PaymentPgProcessor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun processPayment(paymentId: Long, orderId: Long, amount: Long, cardType: String, cardNo: String) {
        val pgResult = pgClient.requestPayment(
            PgPaymentRequest(
                orderId = orderId,
                cardType = CardType.valueOf(cardType),
                cardNo = cardNo,
                amount = amount,
            ),
        )

        log.info("PG 응답 수신. paymentId={}, orderId={}, status={}", paymentId, orderId, pgResult.status)

        when (pgResult.status) {
            PgResultStatus.SUCCESS -> {
                // REQUESTED 상태 유지 (콜백 대기)
            }
            PgResultStatus.TIMEOUT -> {
                // [CR 미반영] Order 상태를 여기서 갱신하지 않는 이유:
                // TIMEOUT은 PG 응답 불명확 상태이므로 복구 스케줄러가 실제 결과를 재확인한다.
                // Order를 PENDING_PAYMENT로 유지해야 복구 흐름이 정상 동작한다.
                txTemplate.executeWithoutResult {
                    val timeoutPayment = paymentRepository.findByIdForUpdate(paymentId)
                        ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다. paymentId=$paymentId")
                    timeoutPayment.markTimeout()
                    paymentRepository.save(timeoutPayment)
                }
            }
            PgResultStatus.FAILED -> {
                log.warn("PG 결제 실패. paymentId={}, orderId={}, reason={}", paymentId, orderId, pgResult.reason)
                txTemplate.executeWithoutResult {
                    val failedPayment = paymentRepository.findByIdForUpdate(paymentId)
                        ?: throw CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다. paymentId=$paymentId")
                    val order = orderRepository.findByIdForUpdate(OrderId(orderId))
                        ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다. orderId=$orderId")
                    val reason = pgResult.reason ?: "PG 결제 실패"
                    failedPayment.markFailed(reason)
                    paymentRepository.save(failedPayment)
                    order.markFailed()
                    orderRepository.save(order)
                    orderOutboxRepository.save(
                        OrderOutbox(
                            eventType = OrderOutboxEventType.PAYMENT_FAILED,
                            orderId = order.id,
                            userId = UserId(order.refUserId.value),
                            reason = reason,
                        ),
                    )
                }
            }
        }
    }
}
