package com.loopers.application.payment

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.payment.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class HandlePaymentCallbackUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handle(callbackInfo: PaymentCallbackInfo) {
        val payment = paymentRepository.findByTransactionKeyForUpdate(callbackInfo.transactionKey)
        if (payment == null) {
            log.warn("콜백 수신했으나 결제를 찾을 수 없습니다. transactionKey={}", callbackInfo.transactionKey)
            return
        }

        if (payment.isTerminal()) {
            log.info("이미 최종 상태인 결제입니다. paymentId={}, status={}", payment.persistenceId, payment.status)
            return
        }

        when (callbackInfo.status) {
            "SUCCESS" -> {
                val approved = payment.approve()
                paymentRepository.save(approved)

                val order = orderRepository.findByIdForUpdate(payment.refOrderId)
                if (order != null) {
                    val completed = order.complete()
                    orderRepository.save(completed)
                }
                log.info("결제 승인 완료. paymentId={}, orderId={}", payment.persistenceId, payment.refOrderId)
            }
            "FAILED" -> {
                val failed = payment.fail(callbackInfo.reason ?: "PG 결제 실패")
                paymentRepository.save(failed)
                log.info("결제 실패 처리. paymentId={}, reason={}", payment.persistenceId, callbackInfo.reason)
            }
            else -> {
                log.warn("알 수 없는 콜백 상태. status={}, transactionKey={}", callbackInfo.status, callbackInfo.transactionKey)
            }
        }
    }
}
