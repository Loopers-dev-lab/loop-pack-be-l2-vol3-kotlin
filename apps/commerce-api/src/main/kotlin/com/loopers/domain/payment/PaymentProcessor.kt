package com.loopers.domain.payment

import com.loopers.domain.order.Order
import org.springframework.stereotype.Component

@Component
class PaymentProcessor(
    private val paymentReader: PaymentReader,
    private val paymentRepository: PaymentRepository,
) {

    fun initiate(order: Order, memberId: Long, cardType: CardType, cardNo: String): Payment =
        paymentRepository.save(
            Payment(
                orderId = requireNotNull(order.id),
                memberId = memberId,
                cardType = cardType,
                cardNo = cardNo,
                amount = order.finalPrice,
            ),
        )

    fun applyRequestResult(paymentId: Long, requestResult: PaymentGateway.RequestResult): Payment {
        val payment = paymentReader.getById(paymentId)

        when (requestResult) {
            is PaymentGateway.RequestResult.Accepted -> payment.markAccepted(
                transactionKey = requestResult.transactionKey,
                reason = requestResult.reason,
            )

            is PaymentGateway.RequestResult.RequestFailed -> payment.markRequestFailed(requestResult.reason)
            is PaymentGateway.RequestResult.Unknown -> payment.markUnknown(requestResult.reason)
        }

        return paymentRepository.save(payment)
    }

    fun applyLookupResult(paymentId: Long, lookupResult: PaymentGateway.LookupResult): Payment {
        val payment = paymentReader.getById(paymentId)

        when (lookupResult) {
            is PaymentGateway.LookupResult.Found -> payment.applyPgResult(
                transactionKey = lookupResult.transactionKey,
                status = lookupResult.status,
                reason = lookupResult.reason,
            )

            is PaymentGateway.LookupResult.NotFound -> {
                if (payment.status == PaymentStatus.REQUESTED || payment.status == PaymentStatus.UNKNOWN) {
                    payment.markRequestFailed("PG 결제 정보를 찾지 못했습니다.")
                }
            }

            is PaymentGateway.LookupResult.Unavailable -> Unit
        }

        return paymentRepository.save(payment)
    }

    fun applyCallback(transactionKey: String, status: PgPaymentStatus, reason: String?): Payment? {
        val payment = paymentReader.findByTransactionKey(transactionKey) ?: return null

        payment.applyPgResult(
            transactionKey = transactionKey,
            status = status,
            reason = reason,
        )

        return paymentRepository.save(payment)
    }
}
