package com.loopers.application.order

import com.loopers.application.payment.PaymentFacade
import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class OrderFacadeTest {

    @Mock
    private lateinit var orderService: OrderService

    @Mock
    private lateinit var productService: ProductService

    @Mock
    private lateinit var brandService: BrandService

    @Mock
    private lateinit var couponService: CouponService

    @Mock
    private lateinit var paymentFacade: PaymentFacade

    private lateinit var orderFacade: OrderFacade

    @BeforeEach
    fun setUp() {
        orderFacade = OrderFacade(orderService, productService, brandService, couponService, paymentFacade)
    }

    @DisplayName("주문 목록을 조회할 때,")
    @Nested
    inner class GetOrders {

        @DisplayName("시작일이 종료일보다 크면, BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenStartAtIsAfterEndAt() {
            // arrange
            val userId = 1L
            val startAt = LocalDateTime.of(2026, 2, 28, 23, 59, 59)
            val endAt = LocalDateTime.of(2026, 2, 1, 0, 0)

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.getOrders(userId, startAt, endAt)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("정상 날짜 범위이면, OrderInfo 목록을 반환한다.")
        @Test
        fun returnsOrderInfoList_whenValidDateRange() {
            // arrange
            val userId = 1L
            val startAt = LocalDateTime.of(2026, 2, 1, 0, 0)
            val endAt = LocalDateTime.of(2026, 2, 28, 23, 59, 59)
            val order = Order(userId = userId)
            ReflectionTestUtils.setField(order, "id", 1L)
            ReflectionTestUtils.setField(order, "createdAt", ZonedDateTime.now())
            val orders = listOf(order)
            whenever(orderService.getOrders(userId, startAt, endAt)).thenReturn(orders)

            // act
            val result = orderFacade.getOrders(userId, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
        }
    }

    @DisplayName("주문 시 결제를 요청할 때,")
    @Nested
    inner class PlaceOrderWithPayment {

        @DisplayName("결제가 PENDING이면, 주문이 정상 생성된다.")
        @Test
        fun createsOrder_whenPaymentIsPending() {
            // arrange
            val order = Order(userId = 1L)
            ReflectionTestUtils.setField(order, "id", 1L)
            whenever(orderService.findByIdempotencyKey(any())).thenReturn(null)
            whenever(productService.getProductsForOrderWithLock(any())).thenReturn(emptyList())
            whenever(brandService.getBrandsByIds(any())).thenReturn(emptyList())
            whenever(orderService.createOrder(any(), any(), any())).thenReturn(order)
            whenever(paymentFacade.requestPayment(any(), any(), any(), any(), any())).thenReturn(
                PaymentInfo(
                    paymentId = 1L,
                    orderId = "1",
                    cardType = "SAMSUNG",
                    cardNo = "1234-5678-9012-3456",
                    amount = 0L,
                    status = PaymentStatus.PENDING,
                    transactionKey = "txn-key-123",
                    failReason = null,
                ),
            )

            // act & assert — 예외 없이 정상 완료
            orderFacade.placeOrder(
                userId = 1L,
                items = emptyList(),
                idempotencyKey = "idem-001",
                cardType = CardType.SAMSUNG,
                cardNo = "1234-5678-9012-3456",
            )

            verify(paymentFacade).requestPayment(eq(1L), any(), eq(CardType.SAMSUNG), eq("1234-5678-9012-3456"), any())
        }

        @DisplayName("결제가 즉시 FAILED이면, 예외를 던져 주문을 롤백한다.")
        @Test
        fun throwsException_whenPaymentFailed() {
            // arrange
            val order = Order(userId = 1L)
            ReflectionTestUtils.setField(order, "id", 1L)
            whenever(orderService.findByIdempotencyKey(any())).thenReturn(null)
            whenever(productService.getProductsForOrderWithLock(any())).thenReturn(emptyList())
            whenever(brandService.getBrandsByIds(any())).thenReturn(emptyList())
            whenever(orderService.createOrder(any(), any(), any())).thenReturn(order)
            whenever(paymentFacade.requestPayment(any(), any(), any(), any(), any())).thenReturn(
                PaymentInfo(
                    paymentId = 1L,
                    orderId = "1",
                    cardType = "SAMSUNG",
                    cardNo = "1234-5678-9012-3456",
                    amount = 0L,
                    status = PaymentStatus.FAILED,
                    transactionKey = null,
                    failReason = "PG 결제 요청에 실패했습니다.",
                ),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(
                    userId = 1L,
                    items = emptyList(),
                    idempotencyKey = "idem-001",
                    cardType = CardType.SAMSUNG,
                    cardNo = "1234-5678-9012-3456",
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.INTERNAL_ERROR)
        }
    }
}
