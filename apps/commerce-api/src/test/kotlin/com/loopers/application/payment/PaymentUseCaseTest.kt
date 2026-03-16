package com.loopers.application.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgPaymentStatus
import com.loopers.infrastructure.payment.PgSimulatorClient
import com.loopers.infrastructure.payment.PgSimulatorProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.ZonedDateTime

class PaymentUseCaseTest {

    private val pgSimulatorClient = mockk<PgSimulatorClient>()
    private val orderStore = FakeOrderStore(createOrder())
    private val paymentStore = FakePaymentStore()
    private val useCase = PaymentUseCase(
        orderReader = OrderReader(orderStore),
        orderRepository = orderStore,
        paymentRepository = paymentStore,
        pgSimulatorClient = pgSimulatorClient,
        pgSimulatorProperties = PgSimulatorProperties(
            baseUrl = "http://localhost:8082",
            callbackUrl = "http://localhost:8080/api/v1/payments/callback",
            connectTimeout = Duration.ofMillis(200),
            readTimeout = Duration.ofMillis(700),
        ),
        transactionTemplate = TransactionTemplate(NoOpTransactionManager()),
    )

    @Nested
    inner class RequestPayment {
        @Test
        fun `PG가_수락하면_주문은_PAYMENT_PENDING_결제는_PENDING_상태가_된다`() {
            every {
                pgSimulatorClient.requestPayment(any(), any())
            } returns PgSimulatorClient.RequestResult.Accepted(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.PENDING,
                reason = null,
            )

            val result = useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.SAMSUNG,
                    cardNo = "1234-5678-1234-5678",
                ),
            )

            assertThat(result.orderStatus).isEqualTo(OrderStatus.PAYMENT_PENDING.name)
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.PENDING.name)
            assertThat(result.transactionKey).isEqualTo("20250816:TR:9577c5")
        }

        @Test
        fun `타임아웃이면_주문은_PAYMENT_PENDING_결제는_UNKNOWN_상태가_된다`() {
            every {
                pgSimulatorClient.requestPayment(any(), any())
            } returns PgSimulatorClient.RequestResult.Unknown("PG 요청 타임아웃 또는 네트워크 오류가 발생했습니다.")

            val result = useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.KB,
                    cardNo = "2222-3333-4444-5555",
                ),
            )

            assertThat(result.orderStatus).isEqualTo(OrderStatus.PAYMENT_PENDING.name)
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.UNKNOWN.name)
        }
    }

    @Nested
    inner class SyncPayment {
        @Test
        fun `수동_동기화로_SUCCESS를_반영하면_주문이_PAID가_된다`() {
            every {
                pgSimulatorClient.requestPayment(any(), any())
            } returns PgSimulatorClient.RequestResult.Accepted(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.PENDING,
                reason = null,
            )

            useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.HYUNDAI,
                    cardNo = "9999-8888-7777-6666",
                ),
            )

            every {
                pgSimulatorClient.getTransaction(1L, "20250816:TR:9577c5")
            } returns PgSimulatorClient.LookupResult.Found(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.SUCCESS,
                reason = "정상 승인되었습니다.",
            )

            val synced = useCase.syncPayment(memberId = 1L, orderId = 1L)

            assertThat(synced.orderStatus).isEqualTo(OrderStatus.PAID.name)
            assertThat(synced.paymentStatus).isEqualTo(PaymentStatus.SUCCESS.name)
        }
    }

    private class FakeOrderStore(
        private var order: Order,
    ) : OrderRepository {
        override fun save(order: Order): Order {
            this.order = order
            return order
        }

        override fun findById(id: Long): Order? = if (order.id == id) order else null

        override fun findAllByMemberId(memberId: Long): List<Order> =
            if (order.memberId == memberId) listOf(order) else emptyList()
    }

    private class FakePaymentStore : PaymentRepository {
        private val payments = linkedMapOf<Long, Payment>()
        private var sequence = 1L

        override fun save(payment: Payment): Payment {
            val persisted = if (payment.id == null) {
                Payment(
                    id = sequence++,
                    orderId = payment.orderId,
                    memberId = payment.memberId,
                    cardType = payment.cardType,
                    cardNo = payment.cardNo,
                    amount = payment.amount,
                    requestedAt = payment.requestedAt,
                    status = payment.status,
                    pgTransactionKey = payment.pgTransactionKey,
                    reason = payment.reason,
                )
            } else {
                payment
            }
            payments[requireNotNull(persisted.id)] = persisted
            return persisted
        }

        override fun findById(id: Long): Payment? = payments[id]

        override fun findLatestByOrderId(orderId: Long): Payment? =
            payments.values.filter { it.orderId == orderId }.maxByOrNull { requireNotNull(it.id) }

        override fun findLatestByOrderId(orderId: Long, memberId: Long): Payment? =
            payments.values
                .filter { it.orderId == orderId && it.memberId == memberId }
                .maxByOrNull { requireNotNull(it.id) }

        override fun findByPgTransactionKey(transactionKey: String): Payment? =
            payments.values.lastOrNull { it.pgTransactionKey == transactionKey }
    }

    private class NoOpTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    private fun createOrder() = Order(
        id = 1L,
        memberId = 1L,
        orderItems = listOf(
            OrderItem(
                id = 1L,
                productId = 1L,
                productName = "결제상품",
                productPrice = 5000L,
                quantity = 1,
            ),
        ),
        totalPrice = 5000L,
        finalPrice = 5000L,
        orderedAt = ZonedDateTime.now(),
    )
}
