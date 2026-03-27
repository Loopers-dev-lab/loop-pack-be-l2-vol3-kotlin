package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import com.loopers.support.event.user.PaymentFailedEvent
import com.loopers.support.event.user.PaymentSucceededEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentCallbackUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handleCallback(command: PaymentCallbackCommand) {
        val payment = paymentRepository.findById(command.paymentId)
        if (payment == null) {
            log.warn("Callback 수신: 존재하지 않는 paymentId={}", command.paymentId)
            return
        }

        if (payment.isTerminal) {
            log.info("Callback 수신: 이미 terminal 상태. paymentId={}, status={}", payment.id, payment.status)
            return
        }

        if (payment.transactionKey != null && payment.transactionKey != command.transactionKey) {
            log.warn(
                "Callback 수신: transactionKey 불일치. paymentId={}, existing={}, incoming={}",
                payment.id,
                payment.transactionKey,
                command.transactionKey,
            )
            return
        }

        when (command.status.uppercase()) {
            "SUCCESS" -> {
                val succeeded = payment.succeed(command.transactionKey)
                if (!paymentRepository.saveIfPending(succeeded)) {
                    log.info("Callback 수신: 이미 terminal로 반영됨. paymentId={}, status=SUCCESS", payment.id)
                    return
                }

                val order = orderRepository.findById(payment.orderId)!!
                val confirmed = order.confirm()
                orderRepository.save(confirmed)
                eventPublisher.publishEvent(
                    PaymentSucceededEvent(
                        paymentId = payment.id!!,
                        orderId = payment.orderId,
                        userId = payment.userId,
                    ),
                )

                log.info("Callback 처리 완료: paymentId={}, status=SUCCESS", payment.id)
            }
            "FAILED" -> {
                val reasonCode = PaymentReasonCode.fromPgReason(command.reason)
                val paymentWithTransactionKey = if (payment.transactionKey == null) {
                    payment.updateTransactionKey(command.transactionKey)
                } else {
                    payment
                }
                val failed = paymentWithTransactionKey.fail(reasonCode)
                if (!paymentRepository.saveIfPending(failed)) {
                    log.info("Callback 수신: 이미 terminal로 반영됨. paymentId={}, status=FAILED", payment.id)
                    return
                }
                eventPublisher.publishEvent(
                    PaymentFailedEvent(
                        paymentId = payment.id!!,
                        orderId = payment.orderId,
                        userId = payment.userId,
                        reasonCode = reasonCode.name,
                    ),
                )

                log.info("Callback 처리 완료: paymentId={}, status=FAILED, reason={}", payment.id, command.reason)
            }
            else -> {
                log.warn("Callback 수신: 알 수 없는 status={}. paymentId={}", command.status, command.paymentId)
            }
        }
    }
}
