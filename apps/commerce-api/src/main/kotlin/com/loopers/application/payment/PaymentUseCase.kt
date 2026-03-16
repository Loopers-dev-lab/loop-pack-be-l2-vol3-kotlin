package com.loopers.application.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgPaymentStatus
import com.loopers.infrastructure.payment.PgSimulatorClient
import com.loopers.infrastructure.payment.PgSimulatorProperties
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class PaymentUseCase(
    private val orderReader: OrderReader,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val pgSimulatorClient: PgSimulatorClient,
    private val pgSimulatorProperties: PgSimulatorProperties,
    private val transactionTemplate: TransactionTemplate,
) {

    fun requestPayment(memberId: Long, command: RequestCommand): PaymentInfo.Detail {
        val initiatedPaymentId = transactionTemplate.execute {
            val order = orderReader.getById(command.orderId)
            order.validateOwner(memberId)
            order.beginPayment()
            orderRepository.save(order)

            paymentRepository.save(
                Payment(
                    orderId = requireNotNull(order.id),
                    memberId = memberId,
                    cardType = command.cardType,
                    cardNo = command.cardNo,
                    amount = order.finalPrice,
                ),
            ).id
        } ?: throw IllegalStateException("결제 생성에 실패했습니다.")

        val requestResult = pgSimulatorClient.requestPayment(
            memberId = memberId,
            request = PgSimulatorClient.Request(
                orderId = command.orderId.toString(),
                cardType = command.cardType,
                cardNo = command.cardNo,
                amount = transactionTemplate.execute { orderReader.getById(command.orderId).finalPrice }
                    ?: throw IllegalStateException("결제 금액을 조회할 수 없습니다."),
                callbackUrl = pgSimulatorProperties.callbackUrl,
            ),
        )

        return transactionTemplate.execute {
            val payment = paymentRepository.findById(initiatedPaymentId)
                ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)
            val order = orderReader.getById(command.orderId)

            when (requestResult) {
                is PgSimulatorClient.RequestResult.Accepted -> payment.markAccepted(
                    transactionKey = requestResult.transactionKey,
                    reason = requestResult.reason,
                )

                is PgSimulatorClient.RequestResult.RequestFailed -> {
                    payment.markRequestFailed(requestResult.reason)
                    order.markPaymentFailed()
                    orderRepository.save(order)
                }

                is PgSimulatorClient.RequestResult.Unknown -> payment.markUnknown(requestResult.reason)
            }

            val savedPayment = paymentRepository.save(payment)
            PaymentInfo.Detail.from(order, savedPayment)
        } ?: throw IllegalStateException("결제 요청 결과 저장에 실패했습니다.")
    }

    fun syncPayment(memberId: Long, orderId: Long): PaymentInfo.Detail {
        val snapshot = transactionTemplate.execute {
            val order = orderReader.getById(orderId)
            order.validateOwner(memberId)
            val payment = paymentRepository.findLatestByOrderId(orderId, memberId)
                ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

            SyncSnapshot(
                paymentId = requireNotNull(payment.id),
                transactionKey = payment.pgTransactionKey,
            )
        } ?: throw IllegalStateException("동기화 대상을 준비할 수 없습니다.")

        val lookupResult = snapshot.transactionKey?.let { pgSimulatorClient.getTransaction(memberId, it) }
            ?: pgSimulatorClient.findLatestTransactionByOrderId(memberId, orderId.toString())

        return transactionTemplate.execute {
            val order = orderReader.getById(orderId)
            order.validateOwner(memberId)
            val payment = paymentRepository.findById(snapshot.paymentId)
                ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

            when (lookupResult) {
                is PgSimulatorClient.LookupResult.Found -> {
                    payment.applyPgResult(
                        transactionKey = lookupResult.transactionKey,
                        status = lookupResult.status,
                        reason = lookupResult.reason,
                    )
                    updateOrderStatus(order, payment.status)
                }

                is PgSimulatorClient.LookupResult.NotFound -> {
                    if (payment.status == PaymentStatus.REQUESTED || payment.status == PaymentStatus.UNKNOWN) {
                        payment.markRequestFailed("PG 결제 정보를 찾지 못했습니다.")
                        updateOrderStatus(order, payment.status)
                    }
                }

                is PgSimulatorClient.LookupResult.Unavailable -> Unit
            }

            val savedPayment = paymentRepository.save(payment)
            orderRepository.save(order)
            PaymentInfo.Detail.from(order, savedPayment)
        } ?: throw IllegalStateException("결제 동기화 반영에 실패했습니다.")
    }

    fun handleCallback(command: CallbackCommand) {
        transactionTemplate.executeWithoutResult {
            val payment = paymentRepository.findByPgTransactionKey(command.transactionKey) ?: return@executeWithoutResult
            val order = orderReader.getById(payment.orderId)

            payment.applyPgResult(
                transactionKey = command.transactionKey,
                status = command.status,
                reason = command.reason,
            )
            updateOrderStatus(order, payment.status)

            paymentRepository.save(payment)
            orderRepository.save(order)
        }
    }

    private fun updateOrderStatus(order: Order, paymentStatus: PaymentStatus) {
        when (paymentStatus) {
            PaymentStatus.SUCCESS -> order.markPaid()
            PaymentStatus.REQUEST_FAILED,
            PaymentStatus.FAILED,
            -> order.markPaymentFailed()

            PaymentStatus.REQUESTED,
            PaymentStatus.PENDING,
            PaymentStatus.UNKNOWN,
            -> Unit
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
}
