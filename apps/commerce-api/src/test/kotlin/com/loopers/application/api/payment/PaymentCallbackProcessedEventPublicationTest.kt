package com.loopers.application.api.payment

import com.loopers.CommerceApiApplication
import com.loopers.application.api.payment.dto.PaymentCallbackCommand
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptStatus
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.payment.ReceiptJpaRepository
import com.loopers.infrastructure.useractionlog.UserActionLogJpaRepository
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@SpringBootTest(
    classes = [
        CommerceApiApplication::class,
        PaymentCallbackProcessedEventPublicationTest.ProbeConfig::class,
    ],
)
@DisplayName("PaymentCallbackProcessedEvent publication")
class PaymentCallbackProcessedEventPublicationTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val orderJpaRepository: OrderJpaRepository,
    private val receiptJpaRepository: ReceiptJpaRepository,
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
    private val committedProbe: CommittedPaymentCallbackProcessedEventProbe,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)
    private val createdOrderIds = mutableSetOf<Long>()
    private val createdTransactionIds = mutableSetOf<String>()

    @BeforeEach
    fun setUp() {
        committedProbe.clear()
    }

    @AfterEach
    fun tearDown() {
        createdTransactionIds.forEach { transactionId ->
            receiptJpaRepository.findByTransactionId(transactionId)?.let { receiptJpaRepository.delete(it) }
        }
        createdOrderIds.forEach { orderId ->
            orderJpaRepository.findById(orderId).ifPresent { orderJpaRepository.delete(it) }
        }
        createdOrderIds.clear()
        createdTransactionIds.clear()
        committedProbe.clear()
    }

    @Test
    @DisplayName("completePayment COMPLETED 커밋 시 committed 이벤트/로그가 1건 저장되고 주문은 PAID로 전이된다")
    fun publishCommittedEventAndLogOnCompleted() {
        val orderId = 92001L
        val transactionId = "TXN_CALLBACK_COMPLETED_92001"
        val amount = BigDecimal("10000.00")
        createPaymentRequestedOrderAndPendingReceipt(orderId, transactionId, amount)

        paymentFacade.completePayment(
            PaymentCallbackCommand(
                transactionId = transactionId,
                orderId = orderId,
                amount = amount,
                status = "COMPLETED",
                reason = null,
            ),
        )

        assertThat(committedProbe.events).hasSize(1)
        val event = committedProbe.events.single()
        assertThat(event.transactionId).isEqualTo(transactionId)
        assertThat(event.orderId).isEqualTo(orderId)
        assertThat(event.amount).isEqualTo(amount.toLong())
        assertThat(event.status).isEqualTo("COMPLETED")
        assertThat(event.reason).isNull()
        assertThat(event.dedupeKey).isEqualTo("payment.callback.processed:$transactionId:COMPLETED")
        assertThat(userActionLogJpaRepository.countByDedupeKey(event.dedupeKey)).isEqualTo(1)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PAID)
        assertThat(receiptJpaRepository.findByTransactionId(transactionId)?.status).isEqualTo(ReceiptStatus.COMPLETED)
    }

    @Test
    @DisplayName("completePayment FAILED 커밋 시 committed 이벤트/로그가 1건 저장되고 주문은 PENDING으로 복구된다")
    fun publishCommittedEventAndLogOnFailed() {
        val orderId = 92002L
        val transactionId = "TXN_CALLBACK_FAILED_92002"
        val amount = BigDecimal("13000.00")
        createPaymentRequestedOrderAndPendingReceipt(orderId, transactionId, amount)

        paymentFacade.completePayment(
            PaymentCallbackCommand(
                transactionId = transactionId,
                orderId = orderId,
                amount = amount,
                status = "FAILED",
                reason = "declined",
            ),
        )

        assertThat(committedProbe.events).hasSize(1)
        val event = committedProbe.events.single()
        assertThat(event.transactionId).isEqualTo(transactionId)
        assertThat(event.orderId).isEqualTo(orderId)
        assertThat(event.amount).isEqualTo(amount.toLong())
        assertThat(event.status).isEqualTo("FAILED")
        assertThat(event.reason).isEqualTo("declined")
        assertThat(event.dedupeKey).isEqualTo("payment.callback.processed:$transactionId:FAILED")
        assertThat(userActionLogJpaRepository.countByDedupeKey(event.dedupeKey)).isEqualTo(1)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
        assertThat(receiptJpaRepository.findByTransactionId(transactionId)?.status).isEqualTo(ReceiptStatus.FAILED)
    }

    @Test
    @DisplayName("completePayment 콜백 검증 실패 시 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnValidationFailure() {
        val orderId = 92003L
        val transactionId = "TXN_CALLBACK_MISMATCH_92003"
        val amount = BigDecimal("20000.00")
        createPaymentRequestedOrderAndPendingReceipt(orderId, transactionId, amount)
        val persistedLogCountBefore = userActionLogJpaRepository.count()

        assertThatThrownBy {
            paymentFacade.completePayment(
                PaymentCallbackCommand(
                    transactionId = transactionId,
                    orderId = orderId,
                    amount = BigDecimal("19999"),
                    status = "COMPLETED",
                    reason = null,
                ),
            )
        }.isInstanceOf(CoreException::class.java)

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PAYMENT_REQUESTED)
        assertThat(receiptJpaRepository.findByTransactionId(transactionId)?.status).isEqualTo(ReceiptStatus.PENDING)
    }

    @Test
    @DisplayName("completePayment 성공 후 외부 트랜잭션 롤백 시 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnRollback() {
        val orderId = 92004L
        val transactionId = "TXN_CALLBACK_ROLLBACK_92004"
        val amount = BigDecimal("22000.00")
        createPaymentRequestedOrderAndPendingReceipt(orderId, transactionId, amount)
        val persistedLogCountBefore = userActionLogJpaRepository.count()

        transactionTemplate.executeWithoutResult { status ->
            paymentFacade.completePayment(
                PaymentCallbackCommand(
                    transactionId = transactionId,
                    orderId = orderId,
                    amount = amount,
                    status = "COMPLETED",
                    reason = null,
                ),
            )
            status.setRollbackOnly()
        }

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PAYMENT_REQUESTED)
        assertThat(receiptJpaRepository.findByTransactionId(transactionId)?.status).isEqualTo(ReceiptStatus.PENDING)
    }

    private fun createPaymentRequestedOrderAndPendingReceipt(
        orderId: Long,
        transactionId: String,
        amount: BigDecimal,
    ) {
        val order = Order.create(id = orderId, userId = 3001L, couponId = null)
        order.markAsPaymentRequested()
        orderJpaRepository.save(order)

        receiptJpaRepository.save(
            Receipt.create(
                orderId = orderId,
                transactionId = transactionId,
                amount = amount,
                cardType = "SAMSUNG",
                cardNo = "1111-2222",
            ),
        )
        createdOrderIds += orderId
        createdTransactionIds += transactionId
    }

    @TestConfiguration
    class ProbeConfig {
        @Bean
        fun committedPaymentCallbackProcessedEventProbe(): CommittedPaymentCallbackProcessedEventProbe =
            CommittedPaymentCallbackProcessedEventProbe()
    }

    class CommittedPaymentCallbackProcessedEventProbe {
        val events: MutableList<PaymentCallbackProcessedEvent> = mutableListOf()

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
        fun onPaymentCallbackProcessed(event: PaymentCallbackProcessedEvent) {
            events.add(event)
        }

        fun clear() {
            events.clear()
        }
    }
}
