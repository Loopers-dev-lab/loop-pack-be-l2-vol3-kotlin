package com.loopers.application.user.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentCallbackUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
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

        when (command.status.uppercase()) {
            "SUCCESS" -> {
                val succeeded = payment.succeed(command.transactionKey)
                paymentRepository.save(succeeded)

                val order = orderRepository.findById(payment.orderId)!!
                val confirmed = order.confirm()
                orderRepository.save(confirmed)

                log.info("Callback 처리 완료: paymentId={}, status=SUCCESS", payment.id)
            }
            "FAILED" -> {
                val reasonCode = PaymentReasonCode.fromPgReason(command.reason)
                val failed = payment.fail(reasonCode)
                paymentRepository.save(failed)

                log.info("Callback 처리 완료: paymentId={}, status=FAILED, reason={}", payment.id, command.reason)
            }
            else -> {
                log.warn("Callback 수신: 알 수 없는 status={}. paymentId={}", command.status, command.paymentId)
            }
        }
    }
}
