package com.loopers.application.payment

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderProductData
import com.loopers.domain.order.model.Order
import com.loopers.domain.outbox.FakeOrderOutboxRepository
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.FakePgClient
import com.loopers.domain.payment.PgPaymentResult
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@DisplayName("결제 전체 흐름 통합 테스트 (Fake 기반)")
class PaymentFlowIntegrationTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderOutboxRepository: FakeOrderOutboxRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var fakePaymentPgProcessor: FakePaymentPgProcessor
    private lateinit var requestPaymentUseCase: RequestPaymentUseCase
    private lateinit var handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase
    private lateinit var recoverPaymentUseCase: RecoverPaymentUseCase
    private lateinit var recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase
    private lateinit var paymentPgProcessor: PaymentPgProcessorImpl

    private val txTemplate = object : TransactionTemplate() {
        override fun <T> execute(action: TransactionCallback<T>): T? {
            return action.doInTransaction(SimpleTransactionStatus())
        }
    }

    @BeforeEach
    fun setUp() {
        TransactionSynchronizationManager.initSynchronization()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        paymentRepository = FakePaymentRepository()
        orderOutboxRepository = FakeOrderOutboxRepository()
        pgClient = FakePgClient()
        fakePaymentPgProcessor = FakePaymentPgProcessor()
        paymentPgProcessor = PaymentPgProcessorImpl(pgClient, paymentRepository, orderRepository, txTemplate, orderOutboxRepository)
        requestPaymentUseCase = RequestPaymentUseCase(orderRepository, paymentRepository, fakePaymentPgProcessor)
        handlePaymentCallbackUseCase = HandlePaymentCallbackUseCase(
            paymentRepository, orderRepository, orderItemRepository, orderOutboxRepository,
        )
        recoverPaymentUseCase = RecoverPaymentUseCase(
            paymentRepository, orderRepository, orderItemRepository, orderOutboxRepository, pgClient, txTemplate,
        )
        recoverAllPaymentsUseCase = RecoverAllPaymentsUseCase(paymentRepository, recoverPaymentUseCase)
    }

    @AfterEach
    fun tearDown() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    private fun flushAfterCommit() {
        val synchronizations = TransactionSynchronizationManager.getSynchronizations().toList()
        TransactionSynchronizationManager.clearSynchronization()
        TransactionSynchronizationManager.initSynchronization()
        synchronizations.forEach { it.afterCommit() }
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
        val saved = orderRepository.save(order)
        saved.assignOrderIdToItems(saved.id)
        orderItemRepository.saveAll(saved.items)
        return saved
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
            val command = defaultRequestCommand(savedOrder.id.value)
            val paymentInfo = requestPaymentUseCase.execute(command)
            flushAfterCommit()

            // assert — afterCommit에서 fakePaymentPgProcessor 호출 인자 계약 검증
            assertThat(fakePaymentPgProcessor.processPaymentCalls).hasSize(1)
            val pgCall = fakePaymentPgProcessor.processPaymentCalls[0]
            assertThat(pgCall.paymentId).isEqualTo(paymentInfo.id)
            assertThat(pgCall.orderId).isEqualTo(command.orderId)
            assertThat(pgCall.amount).isEqualTo(paymentInfo.amount)
            assertThat(pgCall.cardType).isEqualTo(command.cardType)
            assertThat(pgCall.cardNo).isEqualTo(Payment.maskCardNo(command.cardNo))

            // act — paymentPgProcessor(impl)로 실제 PG 처리 시뮬레이션
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
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
            val outboxes = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutbox.OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId.value).isEqualTo(savedOrder.id.value)
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

            val paymentAfterRequest = paymentRepository.findByOrderId(savedOrder.id)!!
            assertThat(paymentAfterRequest.status).isEqualTo(PaymentStatus.TIMEOUT)

            // arrange — PG 조회 결과를 SUCCESS로 변경 (스케줄러 복구 시점)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-RECOVERED-001",
                orderId = savedOrder.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act — 2단계: 스케줄러 복구
            val result = recoverAllPaymentsUseCase.execute()
            flushAfterCommit()

            // assert
            assertThat(result.recovered).isEqualTo(1)
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
            val outboxes = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutbox.OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId.value).isEqualTo(savedOrder.id.value)
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

            val paymentAfterRequest = paymentRepository.findByOrderId(savedOrder.id)!!
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
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.PAID)
            val outboxes = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutbox.OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId.value).isEqualTo(savedOrder.id.value)
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
            val finalPayment = paymentRepository.findByOrderId(savedOrder.id)!!
            val finalOrder = orderRepository.findById(savedOrder.id)!!
            assertThat(finalPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(finalPayment.reason).isEqualTo("카드 한도 초과")
            assertThat(finalOrder.status).isEqualTo(Order.OrderStatus.FAILED)

            // PG 즉시 실패 시에도 PAYMENT_FAILED outbox가 생성되어야 한다
            val outboxes = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutbox.OrderOutboxEventType.PAYMENT_FAILED)
            assertThat(outbox.orderId.value).isEqualTo(savedOrder.id.value)
            assertThat(outbox.reason).isEqualTo("카드 한도 초과")
        }
    }
}
