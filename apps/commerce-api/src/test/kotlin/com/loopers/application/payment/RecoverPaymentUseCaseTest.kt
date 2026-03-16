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
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class RecoverPaymentUseCaseTest {

    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var useCase: RecoverPaymentUseCase

    @BeforeEach
    fun setUp() {
        paymentRepository = FakePaymentRepository()
        orderRepository = FakeOrderRepository()
        pgClient = FakePgClient()
        useCase = RecoverPaymentUseCase(paymentRepository, orderRepository, pgClient)
    }

    private fun createPendingOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
            ),
        )
        val saved = orderRepository.save(order)
        saved.markPendingPayment()
        orderRepository.save(saved)
        return saved
    }

    private fun createTimeoutPaymentForOrder(orderId: Long): Payment {
        val payment = Payment.create(
            orderId = orderId,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 10000L,
        )
        val saved = paymentRepository.save(payment)
        saved.markTimeout()
        return paymentRepository.save(saved)
    }

    @Nested
    @DisplayName("recoverAll 시")
    inner class RecoverAll {

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 SUCCESS이면 Payment SUCCESS, Order PAID로 전환된다")
        fun recoverAll_timeoutPayment_pgSuccess_recovers() {
            // arrange
            val order = createPendingOrder()
            val payment = createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-001",
                orderId = order.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act
            val count = useCase.recoverAll()

            // assert
            assertThat(count).isEqualTo(1)
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 없으면 상태를 변경하지 않는다")
        fun recoverAll_timeoutPayment_pgNoResult_noChange() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = null // PG에 결과 없음

            // act
            val count = useCase.recoverAll()

            // assert
            assertThat(count).isEqualTo(0)
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            val orderAfter = orderRepository.findById(order.id)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
            assertThat(orderAfter.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }
    }

    @Nested
    @DisplayName("recoverByOrderId 시")
    inner class RecoverByOrderId {

        @Test
        @DisplayName("단건 복구: TIMEOUT Payment이고 PG 결과가 SUCCESS이면 복구에 성공한다")
        fun recoverByOrderId_success() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-001",
                orderId = order.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act
            val result = useCase.recoverByOrderId(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("존재하지 않는 orderId로 복구 시 NOT_FOUND 예외가 발생한다")
        fun recoverByOrderId_notFound_throwsException() {
            // act & assert
            val ex = assertThrows<CoreException> {
                useCase.recoverByOrderId(999L)
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
