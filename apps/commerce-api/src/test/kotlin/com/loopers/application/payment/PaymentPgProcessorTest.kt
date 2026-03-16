package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.payment.FakePgClient
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.PgPaymentResult
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class PaymentPgProcessorTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var processor: PaymentPgProcessorImpl

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        paymentRepository = FakePaymentRepository()
        pgClient = FakePgClient()
        processor = PaymentPgProcessorImpl(pgClient, paymentRepository, orderRepository)
    }

    private fun createPendingOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(2),
            ),
        )
        val saved = orderRepository.save(order)
        saved.markPendingPayment()
        orderRepository.save(saved)
        return saved
    }

    private fun createRequestedPayment(orderId: Long): Payment {
        val payment = Payment.create(
            orderId = orderId,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 20000L,
        )
        return paymentRepository.save(payment)
    }

    private fun callProcessPayment(paymentId: Long, orderId: Long, amount: Long = 20000L) {
        processor.processPayment(
            paymentId = paymentId,
            orderId = orderId,
            amount = amount,
            cardType = "SAMSUNG",
            cardNo = "1234-5678-9012-3456",
        )
    }

    @Nested
    @DisplayName("processPayment 시")
    inner class ProcessPayment {

        @Test
        @DisplayName("PG SUCCESS — Payment는 REQUESTED 상태 유지 (콜백 대기)")
        fun processPayment_pgSuccess_paymentRemainsRequested() {
            // arrange
            val order = createPendingOrder()
            val savedPayment = createRequestedPayment(order.id.value)
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = "TR-SUCCESS-001",
                status = PgResultStatus.SUCCESS,
            )

            // act
            callProcessPayment(savedPayment.id, order.id.value)

            // assert — SUCCESS 시 REQUESTED 유지 (콜백으로 SUCCESS 전환 예정)
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.REQUESTED)
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }

        @Test
        @DisplayName("PG TIMEOUT — Payment가 TIMEOUT 상태로 전환되고 Order는 PENDING_PAYMENT로 남는다")
        fun processPayment_pgTimeout_paymentTimeoutAndOrderPendingPayment() {
            // arrange
            val order = createPendingOrder()
            val savedPayment = createRequestedPayment(order.id.value)
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.TIMEOUT,
            )

            // act
            callProcessPayment(savedPayment.id, order.id.value)

            // assert
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }

        @Test
        @DisplayName("PG FAILED — Payment가 FAILED 상태로 전환되고 Order가 FAILED로 전환된다")
        fun processPayment_pgFailed_paymentFailedAndOrderFailed() {
            // arrange
            val order = createPendingOrder()
            val savedPayment = createRequestedPayment(order.id.value)
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.FAILED,
                reason = "카드 한도 초과",
            )

            // act
            callProcessPayment(savedPayment.id, order.id.value)

            // assert
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.reason).isEqualTo("카드 한도 초과")
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)
        }
    }
}
