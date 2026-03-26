package com.loopers.application.payment

import com.loopers.application.event.OutboxEventWriter
import com.loopers.application.event.PaymentStatusChangedEvent
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.coupon.IssuedCouponProcessor
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderPaymentProcessor
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentProcessor
import com.loopers.domain.payment.PaymentReader
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgPaymentStatus
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.OrderPaidItemPayload
import com.loopers.kafka.OrderPaidPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime

@Component
class PaymentUseCase(
    private val orderReader: OrderReader,
    private val orderPaymentProcessor: OrderPaymentProcessor,
    private val paymentReader: PaymentReader,
    private val paymentProcessor: PaymentProcessor,
    private val issuedCouponProcessor: IssuedCouponProcessor,
    private val paymentGateway: PaymentGateway,
    private val transactionTemplate: TransactionTemplate,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val outboxEventWriter: OutboxEventWriter,
) {

    fun requestPayment(memberId: Long, command: RequestCommand): PaymentInfo.Detail {
        val preparedRequest = transactionTemplate.execute {
            val order = orderPaymentProcessor.beginPayment(command.orderId, memberId)
            val payment = paymentProcessor.initiate(order, memberId, command.cardType, command.cardNo)

            PreparedRequest(
                paymentId = requireNotNull(payment.id),
                orderId = requireNotNull(order.id),
                amount = order.finalPrice,
            )
        } ?: throw IllegalStateException("결제 생성에 실패했습니다.")

        val requestResult = paymentGateway.requestPayment(
            memberId = memberId,
            request = PaymentGateway.Request(
                orderId = preparedRequest.orderId.toString(),
                cardType = command.cardType,
                cardNo = command.cardNo,
                amount = preparedRequest.amount,
            ),
        )

        return transactionTemplate.execute {
            val payment = paymentProcessor.applyRequestResult(preparedRequest.paymentId, requestResult)
            val order = orderPaymentProcessor.applyPaymentResult(preparedRequest.orderId, memberId, payment.status)
            syncCouponState(order.couponId, payment.status)
            publishPaymentSideEffects(order, payment.status)
            PaymentInfo.Detail.from(order, payment)
        } ?: throw IllegalStateException("결제 요청 결과 저장에 실패했습니다.")
    }

    fun syncPayment(memberId: Long, orderId: Long): PaymentInfo.Detail {
        val snapshot = transactionTemplate.execute {
            val order = orderReader.getById(orderId)
            order.validateOwner(memberId)
            val payment = paymentReader.getLatestByOrderId(orderId, memberId)

            SyncSnapshot(
                paymentId = requireNotNull(payment.id),
                transactionKey = payment.pgTransactionKey,
            )
        } ?: throw IllegalStateException("동기화 대상을 준비할 수 없습니다.")

        val lookupResult = snapshot.transactionKey?.let { paymentGateway.getTransaction(memberId, it) }
            ?: paymentGateway.findLatestTransactionByOrderId(memberId, orderId.toString())

        return transactionTemplate.execute {
            val payment = paymentProcessor.applyLookupResult(snapshot.paymentId, lookupResult)
            val order = orderPaymentProcessor.applyPaymentResult(orderId, memberId, payment.status)
            syncCouponState(order.couponId, payment.status)
            publishPaymentSideEffects(order, payment.status)
            PaymentInfo.Detail.from(order, payment)
        } ?: throw IllegalStateException("결제 동기화 반영에 실패했습니다.")
    }

    fun handleCallback(command: CallbackCommand) {
        transactionTemplate.executeWithoutResult {
            val payment = paymentProcessor.applyCallback(
                transactionKey = command.transactionKey,
                status = command.status,
                reason = command.reason,
            ) ?: return@executeWithoutResult

            val order = orderPaymentProcessor.applyPaymentResult(payment.orderId, payment.status)
            syncCouponState(order.couponId, payment.status)
            publishPaymentSideEffects(order, payment.status)
        }
    }

    private fun syncCouponState(issuedCouponId: Long?, paymentStatus: PaymentStatus) {
        if (issuedCouponId == null) {
            return
        }

        when (paymentStatus) {
            PaymentStatus.SUCCESS -> issuedCouponProcessor.confirmUseIfReserved(issuedCouponId)
            PaymentStatus.REQUEST_FAILED,
            PaymentStatus.FAILED,
            -> issuedCouponProcessor.releaseIfReserved(issuedCouponId)

            PaymentStatus.REQUESTED,
            PaymentStatus.PENDING,
            PaymentStatus.UNKNOWN,
            -> Unit
        }
    }

    private fun publishPaymentSideEffects(order: Order, paymentStatus: PaymentStatus) {
        val orderId = requireNotNull(order.id)
        applicationEventPublisher.publishEvent(
            PaymentStatusChangedEvent(
                orderId = orderId,
                memberId = order.memberId,
                paymentStatus = paymentStatus.name,
            ),
        )
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.PAYMENT_STATUS_CHANGED,
                memberId = order.memberId,
                targetType = "order",
                targetId = orderId.toString(),
                details = mapOf("paymentStatus" to paymentStatus.name),
            ),
        )

        if (order.status == com.loopers.domain.order.OrderStatus.PAID) {
            outboxEventWriter.append(
                topic = KafkaTopics.ORDER_EVENTS,
                event = IntegrationEvent(
                    eventId = "order-paid:$orderId:1",
                    eventType = "OrderPaid",
                    aggregateType = "order",
                    aggregateId = orderId.toString(),
                    key = orderId.toString(),
                    version = 1L,
                    occurredAt = ZonedDateTime.now(),
                    payload = OrderPaidPayload(
                        orderId = orderId,
                        memberId = order.memberId,
                        items = order.orderItems.map { item ->
                            OrderPaidItemPayload(
                                productId = item.productId,
                                quantity = item.quantity,
                            )
                        },
                    ),
                ),
            )
        }
    }

    data class RequestCommand(
        val orderId: Long,
        val cardType: CardType,
        val cardNo: String,
    )

    data class CallbackCommand(
        val transactionKey: String,
        val status: PgPaymentStatus,
        val reason: String?,
    )

    private data class SyncSnapshot(
        val paymentId: Long,
        val transactionKey: String?,
    )

    private data class PreparedRequest(
        val paymentId: Long,
        val orderId: Long,
        val amount: Long,
    )
}
