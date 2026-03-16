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
import com.loopers.domain.payment.PgTransactionDetail
import com.loopers.domain.payment.model.PaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("결제 전체 흐름 통합 테스트 (Fake 기반)")
class PaymentFlowIntegrationTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var requestPaymentUseCase: RequestPaymentUseCase
    private lateinit var handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase
    private lateinit var recoverPaymentUseCase: RecoverPaymentUseCase
    private lateinit var recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase
    private lateinit var paymentPgProcessor: PaymentPgProcessorImpl

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        paymentRepository = FakePaymentRepository()
        pgClient = FakePgClient()
        paymentPgProcessor = PaymentPgProcessorImpl(pgClient, paymentRepository, orderRepository)
        requestPaymentUseCase = RequestPaymentUseCase(orderRepository, paymentRepository, FakePaymentPgProcessor())
        handlePaymentCallbackUseCase = HandlePaymentCallbackUseCase(paymentRepository, orderRepository)
        recoverPaymentUseCase = RecoverPaymentUseCase(paymentRepository, orderRepository, pgClient)
        recoverAllPaymentsUseCase = RecoverAllPaymentsUseCase(paymentRepository, recoverPaymentUseCase)
    }

    private fun createSavedOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(
                    id = ProductId(1L),
                    name = "상품A",
                    price = Money(BigDecimal("10000")),
                ) to Quantity(1),
            ),
        )
        return orderRepository.save(order)
    }

    private fun defaultRequestCommand(orderId: Long) = PaymentCommand.RequestPayment(
        userId = 1L,
        orderId = orderId,
        cardType = "SAMSUNG",
        cardNo = "1234-5678-9012-3456",
    )

    @Nested
    @DisplayName("정상 결제 흐름")
    inner class NormalPaymentFlow {

        @Test
        @DisplayName("결제 요청(SUCCESS) 후 콜백(SUCCESS) 수신 시 Payment SUCCESS, Order PAID 상태가 된다")
        fun requestThenCallback_success_paymentSuccessOrderPaid() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = "TR-NORMAL-001",
                status = PgResultStatus.SUCCESS,
            )

            // act — 1단계: 결제 요청 (REQUESTED 저장) + afterCommit 시뮬레이션
            val paymentInfo = requestPaymentUseCase.execute(defaultRequestCommand(savedOrder.id.value))
            paymentPgProcessor.processPayment(
                paymentId = paymentInfo.id,
                orderId = savedOrder.id.value,
                amount = paymentInfo.amount,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
            )

            // act — 2단계: 콜백 수신
            handlePaymentCallbackUseCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = savedOrder.id.value,
                    transactionKey = "TR-NORMAL-001",
                    success = true,
                ),
            )

            // assert
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id.value)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }
    }

    @Nested
    @DisplayName("타임아웃 후 스케줄러 복구 흐름")
    inner class TimeoutRecoveryByScheduler {

        @Test
        @DisplayName("결제 요청 후 PG TIMEOUT 처리, 스케줄러가 PG 성공을 확인하면 Payment SUCCESS, Order PAID 상태가 된다")
        fun requestTimeout_schedulerRecovery_paymentSuccessOrderPaid() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.TIMEOUT,
            )

            // act — 1단계: 결제 요청 + afterCommit 시뮬레이션 (TIMEOUT 결과)
            val paymentInfo = requestPaymentUseCase.execute(defaultRequestCommand(savedOrder.id.value))
            paymentPgProcessor.processPayment(
                paymentId = paymentInfo.id,
                orderId = savedOrder.id.value,
                amount = paymentInfo.amount,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
            )

            val paymentAfterRequest = paymentRepository.findByOrderId(savedOrder.id.value)!!
            assertThat(paymentAfterRequest.status).isEqualTo(PaymentStatus.TIMEOUT)

            // arrange — PG 조회 결과를 SUCCESS로 변경 (스케줄러 복구 시점)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-RECOVERED-001",
                orderId = savedOrder.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act — 2단계: 스케줄러 복구
            val recoveredCount = recoverAllPaymentsUseCase.execute()

            // assert
            assertThat(recoveredCount).isEqualTo(1)
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id.value)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }
    }

    @Nested
    @DisplayName("타임아웃 후 콜백 복구 흐름")
    inner class TimeoutRecoveryByCallback {

        @Test
        @DisplayName("결제 요청 후 PG TIMEOUT 처리, PG 콜백(SUCCESS) 도착 시 Payment SUCCESS, Order PAID 상태가 된다")
        fun requestTimeout_callbackSuccess_paymentSuccessOrderPaid() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.TIMEOUT,
            )

            // act — 1단계: 결제 요청 + afterCommit 시뮬레이션 (TIMEOUT 결과)
            val paymentInfo = requestPaymentUseCase.execute(defaultRequestCommand(savedOrder.id.value))
            paymentPgProcessor.processPayment(
                paymentId = paymentInfo.id,
                orderId = savedOrder.id.value,
                amount = paymentInfo.amount,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
            )

            val paymentAfterRequest = paymentRepository.findByOrderId(savedOrder.id.value)!!
            assertThat(paymentAfterRequest.status).isEqualTo(PaymentStatus.TIMEOUT)

            // act — 2단계: SUCCESS 콜백 수신
            handlePaymentCallbackUseCase.execute(
                PaymentCommand.HandleCallback(
                    orderId = savedOrder.id.value,
                    transactionKey = "TR-CALLBACK-001",
                    success = true,
                ),
            )

            // assert
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id.value)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }
    }

    @Nested
    @DisplayName("결제 실패 흐름")
    inner class FailedPaymentFlow {

        @Test
        @DisplayName("PG FAILED 처리 시 Payment FAILED, Order FAILED 상태가 된다")
        fun requestFailed_paymentFailedOrderFailed() {
            // arrange
            val savedOrder = createSavedOrder()
            pgClient.requestPaymentResult = PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.FAILED,
                reason = "카드 한도 초과",
            )

            // act — 결제 요청 + afterCommit 시뮬레이션 (FAILED 결과)
            val paymentInfo = requestPaymentUseCase.execute(defaultRequestCommand(savedOrder.id.value))
            paymentPgProcessor.processPayment(
                paymentId = paymentInfo.id,
                orderId = savedOrder.id.value,
                amount = paymentInfo.amount,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
            )

            // assert
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id.value)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(finalPayment.reason).isEqualTo("카드 한도 초과")
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.FAILED)
        }
    }
}
