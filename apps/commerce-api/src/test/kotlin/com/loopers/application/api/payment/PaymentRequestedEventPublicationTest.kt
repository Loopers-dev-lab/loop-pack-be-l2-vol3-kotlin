package com.loopers.application.api.payment

import com.loopers.CommerceApiApplication
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.PaymentRequestResult
import com.loopers.domain.payment.PaymentStatusCheckResult
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.event.PaymentRequestedEvent
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
import org.springframework.context.annotation.Primary
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@SpringBootTest(
    classes = [
        CommerceApiApplication::class,
        PaymentRequestedEventPublicationTest.ProbeConfig::class,
        PaymentRequestedEventPublicationTest.FakePaymentClientConfig::class,
    ],
)
@DisplayName("PaymentRequestedEvent publication")
class PaymentRequestedEventPublicationTest @Autowired constructor(
    private val paymentFacade: PaymentFacade,
    private val orderJpaRepository: OrderJpaRepository,
    private val receiptJpaRepository: ReceiptJpaRepository,
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
    private val fakePaymentClient: FakePaymentClient,
    private val committedProbe: CommittedPaymentRequestedEventProbe,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @BeforeEach
    fun setUp() {
        fakePaymentClient.reset()
        committedProbe.clear()
    }

    @AfterEach
    fun tearDown() {
        userActionLogJpaRepository.deleteAll()
        receiptJpaRepository.deleteAll()
        orderJpaRepository.deleteAll()
        fakePaymentClient.reset()
        committedProbe.clear()
    }

    @Test
    @DisplayName("requestPayment PENDING 커밋 시 PaymentRequestedEvent 1건이 커밋 후 전달되고 payment.requested 로그가 저장된다")
    fun publishCommittedEventForPending() {
        val userId = 2001L
        val orderId = 91001L
        createPendingOrder(orderId = orderId, userId = userId)
        fakePaymentClient.willReturn(status = "PENDING")

        val receiptInfo = paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")

        assertThat(committedProbe.events).hasSize(1)
        val event = committedProbe.events.single()
        assertThat(event.userId).isEqualTo(userId)
        assertThat(event.orderId).isEqualTo(orderId)
        assertThat(event.receiptId).isEqualTo(receiptInfo.id)
        assertThat(event.transactionId).isEqualTo(receiptInfo.transactionId)
        assertThat(event.amount).isEqualTo(receiptInfo.amount.toLong())
        assertThat(event.receiptStatus).isEqualTo("PENDING")
        assertThat(event.dedupeKey).isEqualTo("payment.requested:$userId:${receiptInfo.transactionId}")
        assertThat(userActionLogJpaRepository.countByDedupeKey(event.dedupeKey)).isEqualTo(1)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PAYMENT_REQUESTED)
    }

    @Test
    @DisplayName("requestPayment 즉시 COMPLETED 커밋 시에도 PaymentRequestedEvent 1건만 전달된다")
    fun publishCommittedEventForImmediateCompleted() {
        val userId = 2002L
        val orderId = 91002L
        createPendingOrder(orderId = orderId, userId = userId)
        fakePaymentClient.willReturn(status = "COMPLETED")

        val receiptInfo = paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")

        assertThat(committedProbe.events).hasSize(1)
        val event = committedProbe.events.single()
        assertThat(event.receiptId).isEqualTo(receiptInfo.id)
        assertThat(event.transactionId).isEqualTo(receiptInfo.transactionId)
        assertThat(event.receiptStatus).isEqualTo("COMPLETED")
        assertThat(userActionLogJpaRepository.countByDedupeKey(event.dedupeKey)).isEqualTo(1)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PAYMENT_REQUESTED)
    }

    @Test
    @DisplayName("requestPayment FAILED 응답에서는 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnFailed() {
        val userId = 2003L
        val orderId = 91003L
        createPendingOrder(orderId = orderId, userId = userId)
        val persistedLogCountBefore = userActionLogJpaRepository.count()
        fakePaymentClient.willReturn(status = "FAILED", reason = "declined")

        assertThatThrownBy {
            paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")
        }.isInstanceOf(CoreException::class.java)

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    @DisplayName("requestPayment CANCELLED 응답에서는 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnCancelled() {
        val userId = 2004L
        val orderId = 91004L
        createPendingOrder(orderId = orderId, userId = userId)
        val persistedLogCountBefore = userActionLogJpaRepository.count()
        fakePaymentClient.willReturn(status = "CANCELLED", reason = "user_cancelled")

        assertThatThrownBy {
            paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")
        }.isInstanceOf(CoreException::class.java)

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    @DisplayName("requestPayment 타임아웃 예외에서는 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnTimeoutException() {
        val userId = 2005L
        val orderId = 91005L
        createPendingOrder(orderId = orderId, userId = userId)
        val persistedLogCountBefore = userActionLogJpaRepository.count()
        fakePaymentClient.willThrow(RuntimeException("timeout"))

        assertThatThrownBy {
            paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")
        }.isInstanceOf(CoreException::class.java)

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    @DisplayName("requestPayment 검증 실패(기존 PENDING receipt)에서는 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnValidationFailure() {
        val userId = 2006L
        val orderId = 91006L
        createPendingOrder(orderId = orderId, userId = userId)
        receiptJpaRepository.save(
            Receipt.create(
                orderId = orderId,
                transactionId = "TXN_EXISTING_$orderId",
                amount = BigDecimal("10000"),
                cardType = "SAMSUNG",
                cardNo = "1234-5678",
            ),
        )
        val persistedLogCountBefore = userActionLogJpaRepository.count()

        assertThatThrownBy {
            paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")
        }.isInstanceOf(CoreException::class.java)

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    @DisplayName("requestPayment 성공 후 외부 트랜잭션 롤백 시 committed 이벤트/로그가 남지 않는다")
    fun noCommittedEventOnRollback() {
        val userId = 2007L
        val orderId = 91007L
        createPendingOrder(orderId = orderId, userId = userId)
        val persistedLogCountBefore = userActionLogJpaRepository.count()
        fakePaymentClient.willReturn(status = "PENDING")

        transactionTemplate.executeWithoutResult { status ->
            paymentFacade.requestPayment(userId, orderId, "SAMSUNG", "1234-5678")
            status.setRollbackOnly()
        }

        assertThat(committedProbe.events).isEmpty()
        assertThat(userActionLogJpaRepository.count()).isEqualTo(persistedLogCountBefore)
        assertThat(orderJpaRepository.findById(orderId).orElseThrow().status).isEqualTo(OrderStatus.PENDING)
    }

    private fun createPendingOrder(orderId: Long, userId: Long) {
        orderJpaRepository.save(Order.create(id = orderId, userId = userId, couponId = null))
    }

    @TestConfiguration
    class ProbeConfig {
        @Bean
        fun committedPaymentRequestedEventProbe(): CommittedPaymentRequestedEventProbe =
            CommittedPaymentRequestedEventProbe()
    }

    @TestConfiguration
    class FakePaymentClientConfig {
        @Bean
        @Primary
        fun fakePaymentClient(): FakePaymentClient = FakePaymentClient()
    }

    class CommittedPaymentRequestedEventProbe {
        val events: MutableList<PaymentRequestedEvent> = mutableListOf()

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
        fun onPaymentRequested(event: PaymentRequestedEvent) {
            events.add(event)
        }

        fun clear() {
            events.clear()
        }
    }

    class FakePaymentClient : PaymentClient {
        private var configuredStatus: String = "PENDING"
        private var configuredReason: String? = null
        private var configuredException: RuntimeException? = null

        fun willReturn(status: String, reason: String? = null) {
            configuredStatus = status
            configuredReason = reason
            configuredException = null
        }

        fun willThrow(exception: RuntimeException) {
            configuredException = exception
        }

        fun reset() {
            configuredStatus = "PENDING"
            configuredReason = null
            configuredException = null
        }

        override fun requestPayment(
            userId: Long,
            transactionId: String,
            orderId: Long,
            amount: BigDecimal,
            cardType: String,
            cardNo: String,
        ): PaymentRequestResult {
            configuredException?.let { throw it }

            return PaymentRequestResult(
                transactionKey = transactionId,
                orderId = orderId.toString(),
                cardType = cardType,
                cardNo = cardNo,
                amount = amount.toLong(),
                status = configuredStatus,
                reason = configuredReason,
            )
        }

        override fun checkPaymentStatus(orderId: Long): PaymentStatusCheckResult {
            throw UnsupportedOperationException("Not required in PaymentRequestedEventPublicationTest")
        }
    }
}
