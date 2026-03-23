package com.loopers.application.payment

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.domain.brand.Brand
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.domain.order.OrderDomainService
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.FakePaymentRepository
import com.loopers.domain.payment.PGOrderPayments
import com.loopers.domain.payment.PGPaymentClient
import com.loopers.domain.payment.PGPaymentRequest
import com.loopers.domain.payment.PGPaymentResponse
import com.loopers.domain.payment.PGTransactionDetail
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentFacadeTest {

    private lateinit var paymentFacade: PaymentFacade
    private lateinit var paymentService: PaymentService
    private lateinit var orderService: OrderService
    private lateinit var fakePGPaymentClient: FakePGPaymentClient
    private lateinit var fakePaymentRepository: FakePaymentRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository

    @BeforeEach
    fun setUp() {
        fakePaymentRepository = FakePaymentRepository()
        fakeOrderRepository = FakeOrderRepository()
        paymentService = PaymentService(fakePaymentRepository)
        orderService = OrderService(fakeOrderRepository, OrderDomainService())
        fakePGPaymentClient = FakePGPaymentClient()
        paymentFacade = PaymentFacade(paymentService, orderService, fakePGPaymentClient)
    }

    private fun createOrderAndGetId(userId: Long = 1L): Long {
        val brand = Brand(name = "나이키").also { setEntityId(it, 1L) }
        val product = Product(
            brandId = 1L,
            name = "에어맥스",
            price = Money(50000),
            stockQuantity = 100,
        ).also { setEntityId(it, 1L) }
        val command = CreateOrderCommand(
            userId = userId,
            products = listOf(product),
            quantities = mapOf(1L to 1),
            brands = mapOf(1L to brand),
        )
        return orderService.createOrder(command).id
    }

    private fun createCriteria(orderId: Long): RequestPaymentCriteria {
        return RequestPaymentCriteria(
            orderId = orderId,
            cardType = "SAMSUNG",
            cardNo = "1234-5678-9012-3456",
        )
    }

    @Nested
    inner class RequestPayment {

        @Test
        @DisplayName("정상적으로 결제 요청이 생성된다")
        fun success() {
            // arrange
            val orderId = createOrderAndGetId()

            // act
            val result = paymentFacade.requestPayment(1L, createCriteria(orderId))

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.REQUESTED.name)
            assertThat(result.transactionKey).isEqualTo("TXN-FAKE-001")
        }

        @Test
        @DisplayName("다른 사용자의 주문이면 FORBIDDEN 예외가 발생한다")
        fun forbiddenForOtherUser() {
            // arrange
            val orderId = createOrderAndGetId(userId = 1L)

            // act
            val result = assertThrows<CoreException> {
                paymentFacade.requestPayment(999L, createCriteria(orderId))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("이미 결제 완료된 주문이면 BAD_REQUEST 예외가 발생한다")
        fun alreadyConfirmedThrowsBadRequest() {
            // arrange
            val orderId = createOrderAndGetId()
            orderService.markConfirmed(orderId)

            // act
            val result = assertThrows<CoreException> {
                paymentFacade.requestPayment(1L, createCriteria(orderId))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("진행 중인 결제가 있으면 BAD_REQUEST 예외가 발생한다")
        fun duplicatePaymentThrowsBadRequest() {
            // arrange
            val orderId = createOrderAndGetId()
            paymentFacade.requestPayment(1L, createCriteria(orderId))

            // act
            val result = assertThrows<CoreException> {
                paymentFacade.requestPayment(1L, createCriteria(orderId))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("PG 호출 실패 시 Payment가 TIMEOUT으로 변경된다")
        fun pgFailureMarksTimeout() {
            // arrange
            val orderId = createOrderAndGetId()
            fakePGPaymentClient.shouldFail = true

            // act
            val result = paymentFacade.requestPayment(1L, createCriteria(orderId))

            // assert
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.TIMEOUT.name)
        }
    }

    @Nested
    inner class HandleCallback {

        @Test
        @DisplayName("SUCCESS 콜백 시 Payment가 APPROVED, Order가 CONFIRMED로 변경된다")
        fun successCallbackUpdatesPaymentAndOrder() {
            // arrange
            val orderId = createOrderAndGetId()
            val paymentResult = paymentFacade.requestPayment(1L, createCriteria(orderId))
            val criteria = PaymentCallbackCriteria(
                transactionKey = paymentResult.transactionKey!!,
                orderId = "ORD-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000,
                status = "SUCCESS",
                reason = "정상 승인",
            )

            // act
            paymentFacade.handleCallback(criteria)

            // assert
            val payment = paymentService.findByTransactionKey(paymentResult.transactionKey!!)
            val order = orderService.findById(orderId)
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.CONFIRMED)
        }

        @Test
        @DisplayName("FAILED 콜백 시 Payment가 REJECTED, Order는 ORDERED 상태를 유지한다")
        fun failedCallbackKeepsOrderOrdered() {
            // arrange
            val orderId = createOrderAndGetId()
            val paymentResult = paymentFacade.requestPayment(1L, createCriteria(orderId))
            val criteria = PaymentCallbackCriteria(
                transactionKey = paymentResult.transactionKey!!,
                orderId = "ORD-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000,
                status = "FAILED",
                reason = "한도 초과",
            )

            // act
            paymentFacade.handleCallback(criteria)

            // assert
            val payment = paymentService.findByTransactionKey(paymentResult.transactionKey!!)
            val order = orderService.findById(orderId)
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.REJECTED)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
        }

        @Test
        @DisplayName("이미 처리된 결제 콜백은 무시된다 (멱등성)")
        fun duplicateCallbackIsIgnored() {
            // arrange
            val orderId = createOrderAndGetId()
            val paymentResult = paymentFacade.requestPayment(1L, createCriteria(orderId))
            val criteria = PaymentCallbackCriteria(
                transactionKey = paymentResult.transactionKey!!,
                orderId = "ORD-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000,
                status = "SUCCESS",
                reason = "정상 승인",
            )
            paymentFacade.handleCallback(criteria)

            // act (중복 콜백)
            paymentFacade.handleCallback(criteria)

            // assert — 예외 없이 정상 처리
            val payment = paymentService.findByTransactionKey(paymentResult.transactionKey!!)
            assertThat(payment.paymentStatus).isEqualTo(PaymentStatus.APPROVED)
        }

        @Test
        @DisplayName("결제 실패 후 같은 주문으로 재결제가 가능하다")
        fun retryPaymentAfterFailure() {
            // arrange
            val orderId = createOrderAndGetId()
            val firstResult = paymentFacade.requestPayment(1L, createCriteria(orderId))
            paymentFacade.handleCallback(
                PaymentCallbackCriteria(
                    transactionKey = firstResult.transactionKey!!,
                    orderId = "ORD-001",
                    cardType = "SAMSUNG",
                    cardNo = "1234-5678-9012-3456",
                    amount = 50000,
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )

            // act — 같은 주문으로 재결제
            val secondResult = paymentFacade.requestPayment(1L, createCriteria(orderId))

            // assert
            assertThat(secondResult.paymentStatus).isEqualTo(PaymentStatus.REQUESTED.name)
            assertThat(orderService.findById(orderId).orderStatus).isEqualTo(OrderStatus.ORDERED)
        }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    /**
     * 테스트용 PGPaymentClient Fake.
     */
    class FakePGPaymentClient : PGPaymentClient {
        var shouldFail = false
        private var sequence = 1

        override fun requestPayment(request: PGPaymentRequest): PGPaymentResponse {
            if (shouldFail) {
                throw CoreException(ErrorType.PG_TIMEOUT, "PG 타임아웃")
            }
            return PGPaymentResponse(
                transactionKey = "TXN-FAKE-${String.format("%03d", sequence++)}",
                status = "PENDING",
                reason = null,
            )
        }

        override fun getPaymentByTransactionKey(userId: Long, transactionKey: String): PGTransactionDetail {
            return PGTransactionDetail(
                transactionKey = transactionKey,
                orderId = "ORD-001",
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 50000,
                status = "SUCCESS",
                reason = "정상 승인",
            )
        }

        override fun getPaymentsByOrderId(userId: Long, orderId: String): PGOrderPayments {
            return PGOrderPayments(
                orderId = orderId,
                transactions = emptyList(),
            )
        }
    }
}
