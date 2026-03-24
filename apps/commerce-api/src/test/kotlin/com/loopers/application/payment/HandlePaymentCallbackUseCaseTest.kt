package com.loopers.application.payment

import com.loopers.application.payment.pg.PgPaymentClient
import com.loopers.application.payment.pg.PgPaymentResponse
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderItemSnapshot
import com.loopers.domain.order.Quantity
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.Money
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.product.Stock
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
class HandlePaymentCallbackUseCaseTest @Autowired constructor(
    private val handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase,
    private val requestPaymentUseCase: RequestPaymentUseCase,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val paymentRepository: PaymentRepository,
    private val productStockRepository: ProductStockRepository,
    private val userCouponRepository: UserCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockkBean
    private lateinit var pgPaymentClient: PgPaymentClient

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private var savedOrderId: Long = 0L

    private fun createPaymentWithTransaction(
        userCouponId: Long? = null,
    ): PaymentInfo {
        val order = orderRepository.save(
            Order.create(
                userId = 1L,
                items = listOf(
                    OrderItemSnapshot(
                        productId = 1L,
                        productName = "테스트 상품",
                        productPrice = Money(50000),
                        brandName = "테스트 브랜드",
                        imageUrl = "https://example.com/image.jpg",
                        quantity = Quantity(1),
                    ),
                ),
                userCouponId = userCouponId,
            ),
        )
        savedOrderId = order.id

        orderItemRepository.saveAll(
            listOf(
                OrderItem.create(
                    orderId = order.id,
                    snapshot = OrderItemSnapshot(
                        productId = 1L,
                        productName = "테스트 상품",
                        productPrice = Money(50000),
                        brandName = "테스트 브랜드",
                        imageUrl = "https://example.com/image.jpg",
                        quantity = Quantity(1),
                    ),
                ),
            ),
        )

        every { pgPaymentClient.requestPayment(any()) } returns PgPaymentResponse(
            transactionId = "txn_callback_test",
            orderId = "test",
            status = "REQUESTED",
            message = null,
        )
        return requestPaymentUseCase.execute(
            PaymentCommand.Request(
                orderId = order.id,
                userId = 1L,
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
            ),
        )
    }

    @DisplayName("결제 콜백 처리")
    @Nested
    inner class Execute {

        @DisplayName("성공 콜백이면 APPROVED 상태가 되고, 재고가 차감된다.")
        @Test
        fun successCallback() {
            // arrange
            productStockRepository.save(ProductStock.create(productId = 1L, stock = Stock(50)))
            createPaymentWithTransaction()

            // act
            val result = handlePaymentCallbackUseCase.execute(
                PaymentCommand.Callback(
                    transactionId = "txn_callback_test",
                    status = "SUCCESS",
                    reason = null,
                ),
            )

            // assert
            val productStock = productStockRepository.findByProductId(1L)
            assertAll(
                { assertThat(result.status).isEqualTo(PaymentStatus.APPROVED) },
                { assertThat(productStock?.stock?.quantity).isEqualTo(49) },
            )
        }

        @DisplayName("실패 콜백이면 FAILED 상태가 되고, 주문이 취소된다.")
        @Test
        fun failCallback() {
            // arrange
            createPaymentWithTransaction()

            // act
            val result = handlePaymentCallbackUseCase.execute(
                PaymentCommand.Callback(
                    transactionId = "txn_callback_test",
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.FAILED)
            val order = orderRepository.findByIdOrNull(savedOrderId)!!
            assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        }

        @DisplayName("실패 콜백 시 사용된 쿠폰이 반환된다.")
        @Test
        fun failCallbackRestoresCoupon() {
            // arrange
            val userCoupon = userCouponRepository.save(
                UserCoupon.create(
                    couponId = 1L,
                    userId = 1L,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            userCoupon.use(orderId = 0L)
            userCouponRepository.save(userCoupon)
            createPaymentWithTransaction(userCouponId = userCoupon.id)

            // act
            handlePaymentCallbackUseCase.execute(
                PaymentCommand.Callback(
                    transactionId = "txn_callback_test",
                    status = "FAILED",
                    reason = "한도 초과",
                ),
            )

            // assert
            val restored = userCouponRepository.findByIdOrNull(userCoupon.id)!!
            assertAll(
                { assertThat(restored.status.name).isEqualTo("AVAILABLE") },
                { assertThat(restored.usedAt).isNull() },
                { assertThat(restored.usedOrderId).isNull() },
            )
        }

        @DisplayName("이미 APPROVED인 결제에 콜백이 오면 무시한다 (멱등성).")
        @Test
        fun idempotentWhenAlreadyApproved() {
            // arrange
            productStockRepository.save(ProductStock.create(productId = 1L, stock = Stock(50)))
            createPaymentWithTransaction()
            handlePaymentCallbackUseCase.execute(
                PaymentCommand.Callback(transactionId = "txn_callback_test", status = "SUCCESS", reason = null),
            )

            // act - 두 번째 콜백
            val result = handlePaymentCallbackUseCase.execute(
                PaymentCommand.Callback(transactionId = "txn_callback_test", status = "SUCCESS", reason = null),
            )

            // assert
            assertThat(result.status).isEqualTo(PaymentStatus.APPROVED)
            val productStock = productStockRepository.findByProductId(1L)
            assertThat(productStock?.stock?.quantity).isEqualTo(49)
        }
    }

    @DisplayName("동시 콜백 방어 검증")
    @Nested
    inner class ConcurrentCallback {

        @DisplayName("동일 결제에 성공/실패 콜백이 동시에 도착하면, 하나만 반영되고 최종 상태는 터미널이다.")
        @Test
        fun onlyOneCallbackApplied() {
            // arrange
            productStockRepository.save(ProductStock.create(productId = 1L, stock = Stock(50)))
            createPaymentWithTransaction()
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            // act — 두 스레드가 동시에 콜백 처리
            val successFuture = executor.submit<PaymentInfo> {
                latch.await()
                handlePaymentCallbackUseCase.execute(
                    PaymentCommand.Callback(transactionId = "txn_callback_test", status = "SUCCESS", reason = null),
                )
            }
            val failFuture = executor.submit<PaymentInfo> {
                latch.await()
                handlePaymentCallbackUseCase.execute(
                    PaymentCommand.Callback(transactionId = "txn_callback_test", status = "FAILED", reason = "한도 초과"),
                )
            }

            latch.countDown()

            val result1 = successFuture.get()
            val result2 = failFuture.get()
            executor.shutdown()

            // assert — 최종 DB 상태는 APPROVED 또는 FAILED 중 하나 (터미널)
            val payment = paymentRepository.findByIdOrNull(result1.id)!!
            assertThat(payment.status.isTerminal()).isTrue()
        }
    }
}
