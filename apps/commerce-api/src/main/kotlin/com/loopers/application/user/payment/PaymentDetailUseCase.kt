package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@Service
class PaymentDetailUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgPaymentPort: PgPaymentPort,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun detail(paymentId: Long, userId: Long): PaymentResult.Detail {
        val payment = paymentRepository.findById(paymentId)
            ?: throw CoreException(ErrorType.PAYMENT_NOT_FOUND)

        if (payment.userId != userId) {
            throw CoreException(ErrorType.PAYMENT_NOT_FOUND)
        }

        val reconciledPayment = if (!payment.isTerminal && payment.transactionKey != null) {
            readRepair(paymentId, payment)
        } else {
            payment
        }

        val order = orderRepository.findById(reconciledPayment.orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)

        return PaymentResult.Detail.from(reconciledPayment, order.status)
    }

    private fun readRepair(
        paymentId: Long,
        payment: com.loopers.domain.payment.Payment,
    ): com.loopers.domain.payment.Payment {
        return try {
            val pgStatus = pgPaymentPort.queryPaymentStatus(payment.transactionKey!!, payment.userId)

            when (pgStatus.status.uppercase()) {
                "SUCCESS" -> {
                    val succeeded = payment.succeed(pgStatus.transactionKey)
                    paymentRepository.save(succeeded)

                    val order = orderRepository.findById(payment.orderId)!!
                    val confirmed = order.confirm()
                    orderRepository.save(confirmed)

                    log.info("Read-repair 완료: paymentId={}, PG status=SUCCESS", paymentId)
                    succeeded
                }
                "FAILED" -> {
                    val reasonCode = PaymentReasonCode.fromPgReason(pgStatus.reason)
                    val failed = payment.fail(reasonCode)
                    paymentRepository.save(failed)

                    log.info("Read-repair 완료: paymentId={}, PG status=FAILED", paymentId)
                    failed
                }
                else -> {
                    log.debug("Read-repair: PG 아직 PENDING. paymentId={}", paymentId)
                    payment
                }
            }
        } catch (e: Exception) {
            log.warn("Read-repair: PG 상태 조회 실패. paymentId={}, error={}", paymentId, e.message)
            payment
        }
    }
}
