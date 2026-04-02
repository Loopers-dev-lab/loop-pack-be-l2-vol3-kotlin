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
import com.loopers.domain.outbox.model.OrderOutbox.OrderOutboxEventType
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.FakePgClient
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import com.loopers.domain.payment.model.CardType
import com.loopers.domain.payment.model.Payment
import com.loopers.domain.payment.model.PaymentStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

class RecoverPaymentUseCaseTest {

    private lateinit var paymentRepository: FakePaymentRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var orderOutboxRepository: FakeOrderOutboxRepository
    private lateinit var pgClient: FakePgClient
    private lateinit var recoverPaymentUseCase: RecoverPaymentUseCase
    private lateinit var recoverAllPaymentsUseCase: RecoverAllPaymentsUseCase

    // afterCommit 콜백 내부의 새 트랜잭션 실행을 즉시 처리하는 TransactionTemplate stub
    private val immediateTxTemplate = TransactionTemplate().apply {
        setTransactionManager(
            object : PlatformTransactionManager {
                override fun getTransaction(definition: TransactionDefinition?) =
                    DefaultTransactionStatus(
                        "test-tx",
                        null,
                        true,
                        true,
                        false,
                        false,
                        false,
                        null,
                    )

                override fun commit(status: TransactionStatus) {
                    /* no-op: 테스트용 stub */
                }

                override fun rollback(status: TransactionStatus) {
                    /* no-op: 테스트용 stub */
                }
            },
        )
    }

    @BeforeEach
    fun setUp() {
        paymentRepository = FakePaymentRepository()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        orderOutboxRepository = FakeOrderOutboxRepository()
        pgClient = FakePgClient()
        // TransactionSynchronizationManager 활성화: registerSynchronization 호출 허용
        TransactionSynchronizationManager.initSynchronization()
        recoverPaymentUseCase = RecoverPaymentUseCase(
            paymentRepository, orderRepository, orderItemRepository, orderOutboxRepository, pgClient, immediateTxTemplate,
        )
        recoverAllPaymentsUseCase = RecoverAllPaymentsUseCase(paymentRepository, recoverPaymentUseCase)
    }

    @AfterEach
    fun tearDown() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    /**
     * execute() 호출 후 등록된 afterCommit 콜백을 즉시 실행한다.
     * 실제 트랜잭션 커밋 시 Spring이 호출하는 동작을 단위 테스트에서 시뮬레이션한다.
     */
    private fun executeAndFlush(orderId: Long): Boolean {
        val result = recoverPaymentUseCase.execute(orderId)
        flushAfterCommit()
        return result
    }

    /**
     * RecoverAllPaymentsUseCase 실행 후 등록된 모든 afterCommit 콜백을 즉시 실행한다.
     */
    private fun executeAllAndFlush(): RecoveryResult {
        val result = recoverAllPaymentsUseCase.execute()
        flushAfterCommit()
        return result
    }

    private fun flushAfterCommit() {
        val synchronizations = TransactionSynchronizationManager.getSynchronizations().toList()
        TransactionSynchronizationManager.clearSynchronization()
        TransactionSynchronizationManager.initSynchronization()
        synchronizations.forEach { it.afterCommit() }
    }

    private fun createPendingOrder(): Order {
        val order = Order.create(
            UserId(1L),
            listOf(
                OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
            ),
        )
        val saved = orderRepository.save(order)
        saved.assignOrderIdToItems(saved.id)
        orderItemRepository.saveAll(saved.items)
        saved.markPendingPayment()
        orderRepository.save(saved)
        return saved
    }

    private fun createRequestedPaymentForOrder(orderId: Long): Payment {
        val payment = Payment.create(
            orderId = orderId,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            amount = 10000L,
        )
        return paymentRepository.save(payment)
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
        @DisplayName("REQUESTED Payment이고 PG 조회 결과가 SUCCESS이면 결제 성공으로 전환된다")
        fun execute_requestedPayment_pgSuccess_recovers() {
            // arrange
            val order = createPendingOrder()
            createRequestedPaymentForOrder(order.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-001",
                orderId = order.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act
            val result = executeAndFlush(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            val updatedOrder = requireNotNull(orderRepository.findById(order.id)) { "주문(${order.id})이 존재해야 합니다" }
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)

            // outbox 검증
            val outboxes = orderOutboxRepository.findAllUnpublished(100)
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId).isEqualTo(order.id)
            assertThat(outbox.userId).isEqualTo(order.refUserId)
        }

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
            val result = executeAndFlush(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            val updatedOrder = requireNotNull(orderRepository.findById(order.id)) { "주문(${order.id})이 존재해야 합니다" }
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)

            // outbox 검증
            val outboxes = orderOutboxRepository.findAllUnpublished(100)
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutboxEventType.PAYMENT_COMPLETED)
            assertThat(outbox.orderId).isEqualTo(order.id)
            assertThat(outbox.userId).isEqualTo(order.refUserId)
        }

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 없으면 상태를 변경하지 않고 true를 반환한다 (복구 시도 시작 의미)")
        fun execute_timeoutPayment_pgNoResult_returnsTrue() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = null

            // act
            val result = executeAndFlush(order.id.value)

            // assert: execute()는 복구 시도 시작 여부를 반환 → true, 상태는 미변경
            assertThat(result).isTrue()
            val payment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            assertThat(payment.status).isEqualTo(PaymentStatus.TIMEOUT)
        }

        @Test
        @DisplayName("Payment가 없으면 false를 반환한다")
        fun execute_noPayment_returnsFalse() {
            // act
            val result = executeAndFlush(999L)

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
            val result = executeAndFlush(order.id.value)

            // assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("PG SUCCESS이지만 주문 항목이 없으면 outbox가 생성되지 않는다")
        fun execute_pgSuccess_emptyOrderItems_noOutboxCreated() {
            // arrange — orderItems 없이 주문/결제 생성
            val order = Order.create(
                UserId(1L),
                listOf(
                    OrderProductData(ProductId(1L), "상품A", Money(BigDecimal("10000"))) to Quantity(1),
                ),
            )
            val saved = orderRepository.save(order)
            saved.markPendingPayment()
            orderRepository.save(saved)
            // orderItems를 저장하지 않음 — 비정상 상태
            createRequestedPaymentForOrder(saved.id.value)
            pgClient.transactionDetail = PgTransactionDetail(
                transactionKey = "TR-EMPTY",
                orderId = saved.id.value,
                status = PgResultStatus.SUCCESS,
            )

            // act — afterCommit 내부에서 IllegalStateException 발생, 외부 catch에서 처리
            val result = executeAndFlush(saved.id.value)

            // assert — execute() 자체는 true (복구 시도 시작), outbox는 미생성
            assertThat(result).isTrue()
            assertThat(orderOutboxRepository.findAllUnpublished(100)).isEmpty()
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
            val result = executeAndFlush(order.id.value)

            // assert
            assertThat(result).isTrue()
            val updatedPayment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            val updatedOrder = requireNotNull(orderRepository.findById(order.id)) { "주문(${order.id})이 존재해야 합니다" }
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.FAILED)

            // outbox 검증
            val outboxes = orderOutboxRepository.findAllUnpublished(100)
            assertThat(outboxes).hasSize(1)
            val outbox = outboxes.single()
            assertThat(outbox.eventType).isEqualTo(OrderOutboxEventType.PAYMENT_FAILED)
            assertThat(outbox.orderId).isEqualTo(order.id)
            assertThat(outbox.userId).isEqualTo(order.refUserId)
            assertThat(outbox.reason).isEqualTo("카드 한도 초과")
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
            val result = executeAllAndFlush()

            // assert
            assertThat(result.recovered).isEqualTo(1)
            val updatedPayment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            val updatedOrder = requireNotNull(orderRepository.findById(order.id)) { "주문(${order.id})이 존재해야 합니다" }
            assertThat(updatedPayment.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(updatedOrder.status).isEqualTo(Order.OrderStatus.PAID)
        }

        @Test
        @DisplayName("TIMEOUT Payment이고 PG 조회 결과가 없으면 복구 시도 건수 1을 반환하고 상태는 변경하지 않는다")
        fun execute_timeoutPayment_pgNoResult_returnsAttemptCount() {
            // arrange
            val order = createPendingOrder()
            createTimeoutPaymentForOrder(order.id.value)
            pgClient.transactionDetail = null

            // act
            val result = executeAllAndFlush()

            // assert: execute()가 복구 시도 시작 여부를 반환하므로 attempted=1, 상태는 미변경
            assertThat(result.attempted).isEqualTo(1)
            val payment = requireNotNull(paymentRepository.findByOrderId(order.id)) { "주문(${order.id})에 대한 결제가 존재해야 합니다" }
            val orderAfter = requireNotNull(orderRepository.findById(order.id)) { "주문(${order.id})이 존재해야 합니다" }
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
                saved.assignOrderIdToItems(saved.id)
                orderItemRepository.saveAll(saved.items)
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

            // act — order1 afterCommit 예외가 catch되고 order2는 정상 처리되어야 함
            val result = executeAllAndFlush()

            // assert — 두 건 모두 execute()가 true 반환(복구 시도 시작) → attempted == 2
            //           order1은 afterCommit에서 PG 예외 → 상태 미변경, order2는 SUCCESS 전환
            assertThat(result.attempted).isEqualTo(2)
            val payment1 = requireNotNull(paymentRepository.findByOrderId(order1.id)) { "주문(${order1.id})에 대한 결제가 존재해야 합니다" }
            val order1After = requireNotNull(orderRepository.findById(order1.id)) { "주문(${order1.id})이 존재해야 합니다" }
            assertThat(payment1.status).isEqualTo(PaymentStatus.TIMEOUT)
            assertThat(order1After.status).isEqualTo(Order.OrderStatus.PENDING_PAYMENT)
            val payment2 = requireNotNull(paymentRepository.findByOrderId(order2.id)) { "주문(${order2.id})에 대한 결제가 존재해야 합니다" }
            assertThat(payment2.status).isEqualTo(PaymentStatus.SUCCESS)
        }
    }
}
