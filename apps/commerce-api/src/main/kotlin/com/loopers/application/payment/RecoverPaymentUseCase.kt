package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.outbox.model.OrderOutboxEventType
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class RecoverPaymentUseCase(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val pgClient: PgClient,
    private val txTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(orderId: Long): Boolean {
        // 1단계: 트랜잭션 안에서 상태 검증 (ForUpdate 락)
        val payment = paymentRepository.findByOrderIdForUpdate(OrderId(orderId)) ?: return false
        if (payment.status != PaymentStatus.REQUESTED && payment.status != PaymentStatus.TIMEOUT) return false

        // afterCommit 콜백 등록: PG 조회와 상태 반영은 트랜잭션 커밋 후 실행
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                try {
                    // 2단계: 트랜잭션 밖에서 PG 조회
                    val detail = pgClient.getTransactionByOrderId(orderId)
                    if (detail == null) {
                        log.info("PG 트랜잭션 미확인. 다음 복구 주기에 재시도. orderId={}", orderId)
                        return
                    }

                    // 3단계: 새 트랜잭션으로 상태 반영
                    txTemplate.executeWithoutResult {
                        val freshPayment = paymentRepository.findByOrderIdForUpdate(OrderId(orderId))
                            ?: return@executeWithoutResult
                        // 다른 프로세스(콜백 등)가 이미 처리했는지 재검증
                        if (freshPayment.status != PaymentStatus.REQUESTED &&
                            freshPayment.status != PaymentStatus.TIMEOUT
                        ) {
                            return@executeWithoutResult
                        }

                        val order = orderRepository.findByIdForUpdate(OrderId(orderId))
                            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

                        when (detail.status) {
                            PgResultStatus.SUCCESS -> {
                                freshPayment.markSuccess(detail.transactionKey)
                                paymentRepository.save(freshPayment)
                                order.markPaid()
                                orderRepository.save(order)
                                val orderItems = orderItemRepository.findAllByOrderId(OrderId(orderId))
                                if (orderItems.isEmpty()) {
                                    log.warn("결제 복구 성공했으나 주문 항목이 없음. orderId={}", orderId)
                                }
                                orderOutboxRepository.saveAll(
                                    orderItems.map { item ->
                                        OrderOutbox(
                                            eventType = OrderOutboxEventType.PAYMENT_COMPLETED,
                                            orderId = OrderId(orderId),
                                            userId = UserId(order.refUserId.value),
                                            totalAmount = Money(freshPayment.amount.toBigDecimal()),
                                            productId = ProductId(item.refProductId.value),
                                            quantity = item.quantity.value,
                                        )
                                    },
                                )
                            }
                            PgResultStatus.FAILED -> {
                                val reason = detail.reason ?: "PG 결제 실패"
                                freshPayment.markFailed(reason)
                                paymentRepository.save(freshPayment)
                                order.markFailed()
                                orderRepository.save(order)
                                orderOutboxRepository.save(
                                    OrderOutbox(
                                        eventType = OrderOutboxEventType.PAYMENT_FAILED,
                                        orderId = OrderId(orderId),
                                        userId = UserId(order.refUserId.value),
                                        reason = reason,
                                    ),
                                )
                            }
                            PgResultStatus.TIMEOUT -> {
                                // TIMEOUT 유지 - 다음 복구 주기에 재시도
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.warn("결제 복구 afterCommit 처리 실패. orderId={}", orderId, e)
                }
            }
        })

        // "복구 시도를 시작했다"는 의미로 true 반환 (afterCommit에서 실제 결과를 알 수 없음)
        return true
    }
}
