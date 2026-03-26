package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PgPaymentPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentReadRepairService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val pgPaymentPort: PgPaymentPort,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun repair(payment: Payment): Payment {
        val paymentId = payment.id!!
        val pgStatus = try {
            pgPaymentPort.queryPaymentStatus(payment.transactionKey!!, payment.userId)
        } catch (e: Exception) {
            log.warn("Read-repair: PG 상태 조회 실패. paymentId={}, error={}", paymentId, e.message)
            return payment
        }

        return when (pgStatus.status.uppercase()) {
            "SUCCESS" -> {
                val succeeded = payment.succeed(pgStatus.transactionKey)
                if (!paymentRepository.saveIfPending(succeeded)) {
                    log.info("Read-repair: 이미 terminal로 반영됨. paymentId={}, PG status=SUCCESS", paymentId)
                    paymentRepository.findById(paymentId) ?: payment
                } else {
                    val order = orderRepository.findById(payment.orderId)!!
                    val confirmed = order.confirm()
                    orderRepository.save(confirmed)

                    log.info("Read-repair 완료: paymentId={}, PG status=SUCCESS", paymentId)
                    succeeded
                }
            }
            "FAILED" -> {
                val reasonCode = PaymentReasonCode.fromPgReason(pgStatus.reason)
                val failed = payment.fail(reasonCode)
                if (!paymentRepository.saveIfPending(failed)) {
                    log.info("Read-repair: 이미 terminal로 반영됨. paymentId={}, PG status=FAILED", paymentId)
                    paymentRepository.findById(paymentId) ?: payment
                } else {
                    log.info("Read-repair 완료: paymentId={}, PG status=FAILED", paymentId)
                    failed
                }
            }
            else -> {
                log.debug("Read-repair: PG 아직 PENDING. paymentId={}", paymentId)
                payment
            }
        }
    }
}
