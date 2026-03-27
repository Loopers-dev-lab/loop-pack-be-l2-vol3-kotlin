package com.loopers.application.payment

import com.loopers.application.payment.support.PaymentCouponStateSyncer
import com.loopers.application.payment.support.PaymentSideEffectPublisher
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderPaymentProcessor
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentProcessor
import com.loopers.domain.payment.PaymentReader
import com.loopers.domain.payment.PgPaymentStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class PaymentUseCase(
    private val orderReader: OrderReader,
    private val orderPaymentProcessor: OrderPaymentProcessor,
    private val paymentReader: PaymentReader,
    private val paymentProcessor: PaymentProcessor,
    private val paymentGateway: PaymentGateway,
    private val transactionTemplate: TransactionTemplate,
    private val paymentCouponStateSyncer: PaymentCouponStateSyncer,
    private val paymentSideEffectPublisher: PaymentSideEffectPublisher,
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
            paymentCouponStateSyncer.sync(order.couponId, payment.status)
            paymentSideEffectPublisher.publish(order, payment.status)
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
            paymentCouponStateSyncer.sync(order.couponId, payment.status)
            paymentSideEffectPublisher.publish(order, payment.status)
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
            paymentCouponStateSyncer.sync(order.couponId, payment.status)
            paymentSideEffectPublisher.publish(order, payment.status)
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
