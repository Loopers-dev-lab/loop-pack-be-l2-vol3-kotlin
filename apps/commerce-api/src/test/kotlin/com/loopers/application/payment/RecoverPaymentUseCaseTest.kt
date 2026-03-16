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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RecoverPaymentUseCaseTest {

    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var recoverPaymentUseCase: RecoverPaymentUseCase
    private lateinit var recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase

    @BeforeEach
    fun setUp() {
        paymentRepository = FakePaymentRepository()
        orderRepository = FakeOrderRepository()
        pgClient = FakePgClient()
        recoverPaymentUseCase = RecoverPaymentUseCase(paymentRepository, orderRepository, pgClient)
        recoverAllPaymentsUseCase = RecoverAllPaymentsUseCase(paymentRepository, recoverPaymentUseCase)
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
    @DisplayName("RecoverPaymentUseCase.execute 시")
    inner class Execute {

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 SUCCESS이면 Payment SUCCESS, Order PAID로 전환된다")
        fun execute_timeoutPayment_pgSuccess_recovers() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-001",
                orderId = order.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act
            val result = recoverPaymentUseCase.execute(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 없으면 상태를 변경하지 않고 false를 반환한다")
        fun execute_timeoutPayment_pgNoResult_returnsFalse() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = null

            // act
            val result = recoverPaymentUseCase.execute(order.id.value)

            // assert
            assertThat(result).isFalse()
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
        }

        @Test
        @DisplayName("Payment가 없으면 false를 반환한다")
        fun execute_noPayment_returnsFalse() {
            // act
            val result = recoverPaymentUseCase.execute(999L)

            // assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("Payment 상태가 REQUESTED/TIMEOUT이 아니면 false를 반환한다")
        fun execute_paymentStatusNotRecoverable_returnsFalse() {
            // arrange
            val order = createPendingOrder()
            val payment = Payment.create(
                orderId = order.id.value,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
            )
            val saved = paymentRepository.save(payment)
            saved.markSuccess("TR-001")
            paymentRepository.save(saved)

            // act
            val result = recoverPaymentUseCase.execute(order.id.value)

            // assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("PG 조회 결과가 FAILED이면 Payment FAILED, Order FAILED로 전환된다")
        fun execute_pgFailed_marksFailedState() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "",
                orderId = order.id.value,
                status = PgResultStatus.FAILED,
                reason = "카드 한도 초과",
            )

            // act
            val result = recoverPaymentUseCase.execute(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)
        }
    }

    @Nested
    @DisplayName("RecoverAllPaymentsUseCase.execute 시")
    inner class RecoverAll {

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 SUCCESS이면 복구 건수를 반환한다")
        fun execute_timeoutPayment_pgSuccess_recovers() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-001",
                orderId = order.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act
            val count = recoverAllPaymentsUseCase.execute()

            // assert
            assertThat(count).isEqualTo(1)
            val updatedPayment = paymentRepository.findByOrderId(order.id.value)!!
            val updatedOrder = orderRepository.findById(order.id)!!
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 없으면 복구 건수 0을 반환한다")
        fun execute_timeoutPayment_pgNoResult_returnsZero() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = null

            // act
            val count = recoverAllPaymentsUseCase.execute()

            // assert
            assertThat(count).isEqualTo(0)
            val payment = paymentRepository.findByOrderId(order.id.value)!!
            val orderAfter = orderRepository.findById(order.id)!!
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
            assertThat(orderAfter.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
        }

        @Test
        @DisplayName("한 건 복구 실패 시 나머지 건은 계속 처리된다")
        fun execute_oneFailure_continuesOthers() {
            // arrange — 2건 설정
            val order1 = createPendingOrder()
            createTimeoutPaymentForOrder(order1.id.value)

            val order2 = Order.create(
                UserId(1L),
                listOf(
                    OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
                ),
            ).let { o ->
                val saved = orderRepository.save(o)
                saved.markPendingPayment()
                orderRepository.save(saved)
                saved
            }
            createTimeoutPaymentForOrder(order2.id.value)

            // 첫 번째 orderId는 PG 예외, 두 번째 orderId는 SUCCESS 반환
            pgClient.transactionDetailExceptionByOrderId[order1.id.value] = RuntimeException("PG 연결 오류")
            pgClient.transactionDetailByOrderId[order2.id.value] = PgTransactionDetail(
                transactionKey = "TR-002",
                orderId = order2.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act — order1 예외가 전파되지 않고 order2는 정상 처리되어야 함
            val count = recoverAllPaymentsUseCase.execute()

            // assert — order1 실패(예외 → skip), order2 성공 → count == 1
            assertThat(count).isEqualTo(1)
            val payment2 = paymentRepository.findByOrderId(order2.id.value)!!
            assertThat(payment2.status).isEqualTo(PaymentStatus.SUCCESS)
        }
    }
}
