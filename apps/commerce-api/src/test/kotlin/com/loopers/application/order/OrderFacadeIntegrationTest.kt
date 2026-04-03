package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.StockReservationRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import java.util.UUID
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.time.ZonedDateTime

@SpringBootTest
class OrderFacadeIntegrationTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val orderService: OrderService,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val stockReservationRepository: StockReservationRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var paymentGateway: PaymentGateway

    @BeforeEach
    fun setUpPaymentGateway() {
        whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
            PaymentGatewayResponse(transactionKey = "txn-test", status = "PENDING", reason = null),
        )
        whenever(paymentGateway.getTransactionsByOrderId(any(), any())).thenReturn(emptyList())
    }

    companion object {
        private val TEST_CARD_TYPE = CardType.SAMSUNG
        private const val TEST_CARD_NO = "1234-5678-9012-3456"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(brand: Brand, name: String = "에어맥스", price: Money = Money.of(100000L), stock: Int = 100): Product {
        val product = productRepository.save(
            Product(name = name, description = "러닝화", price = price, likes = LikeCount.of(0), stockQuantity = StockQuantity.of(stock), brandId = brand.id),
        )
        stockReservationRepository.setStock(product.id, stock)
        return product
    }

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        discountType: DiscountType = DiscountType.FIXED_AMOUNT,
        discountValue: Long = 5000L,
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = Discount(discountType, discountValue),
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    private fun issueCoupon(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.save(IssuedCoupon(couponId = couponId, userId = userId))
    }

    private fun issueEntryToken(userId: Long): String {
        val token = UUID.randomUUID().toString()
        entryTokenRepository.issue(userId, token, 300L)
        return token
    }

    @DisplayName("Redis 재고 선점 기반 주문")
    @Nested
    inner class RedisStockReservation {

        @DisplayName("Redis 재고 선점 성공 후 주문이 생성된다.")
        @Test
        fun createsOrderAfterRedisStockReservation() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, stock = 10)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(2)))

            // act
            orderFacade.placeOrder(userId, items, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            assertThat(orders).hasSize(1)
        }

        @DisplayName("Redis 재고 부족 시 주문이 실패한다.")
        @Test
        fun failsOrder_whenRedisStockInsufficient() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, stock = 1)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(5)))

            // act & assert
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("여러 상품 주문 중 일부 재고 부족 시 이미 선점한 재고가 복원된다.")
        @Test
        fun restoresReservedStock_whenPartialStockInsufficient() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product1 = createProduct(brand, name = "상품A", stock = 10)
            val product2 = createProduct(brand, name = "상품B", stock = 1)
            val items = listOf(
                OrderPlaceCommand(productId = product1.id, quantity = Quantity.of(3)),
                OrderPlaceCommand(productId = product2.id, quantity = Quantity.of(5)),
            )

            // act
            assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert: 첫 번째 상품 재고가 복원됨
            val restored = stockReservationRepository.reserve(product1.id, 10)
            assertThat(restored).isTrue()
        }

        @DisplayName("결제 실패 시 Redis 재고가 복원된다.")
        @Test
        fun restoresRedisStock_whenPaymentFails() {
            // arrange
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-fail", status = "FAILED", reason = "잔액 부족"),
            )
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, stock = 10)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(3)))

            // act
            assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert: 재고 복원됨 (10개 전량 선점 가능)
            val canReserve = stockReservationRepository.reserve(product.id, 10)
            assertThat(canReserve).isTrue()
        }
    }

    @DisplayName("Redis 쿠폰 선점 기반 주문")
    @Nested
    inner class RedisCouponReservation {

        @DisplayName("Redis 쿠폰 선점 성공 후 할인이 적용된 주문이 생성된다.")
        @Test
        fun createsOrderWithCouponDiscount() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, price = Money.of(100000L))
            val coupon = createCoupon(discountType = DiscountType.FIXED_AMOUNT, discountValue = 5000L)
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(2)))

            // act
            orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            val order = orders.first()
            assertAll(
                { assertThat(order.totalAmount).isEqualTo(Money.of(200000L)) },
                { assertThat(order.discountAmount).isEqualTo(Money.of(5000L)) },
                { assertThat(order.paymentAmount).isEqualTo(Money.of(195000L)) },
                { assertThat(order.couponId).isEqualTo(coupon.id) },
            )
        }

        @DisplayName("정률 할인 쿠폰이 적용된 주문이 생성된다.")
        @Test
        fun createsOrderWithPercentageDiscount() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, price = Money.of(100000L))
            val coupon = createCoupon(discountType = DiscountType.PERCENTAGE, discountValue = 10L)
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(2)))

            // act
            orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            val order = orders.first()
            assertAll(
                { assertThat(order.totalAmount).isEqualTo(Money.of(200000L)) },
                { assertThat(order.discountAmount).isEqualTo(Money.of(20000L)) },
                { assertThat(order.paymentAmount).isEqualTo(Money.of(180000L)) },
            )
        }

        @DisplayName("쿠폰 없이 주문하면, 할인 없이 주문이 생성된다.")
        @Test
        fun createsOrderWithoutDiscount_whenNoCoupon() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, price = Money.of(100000L))
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(2)))

            // act
            orderFacade.placeOrder(userId, items, null, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            val order = orders.first()
            assertAll(
                { assertThat(order.totalAmount).isEqualTo(Money.of(200000L)) },
                { assertThat(order.discountAmount).isEqualTo(Money.ZERO) },
                { assertThat(order.paymentAmount).isEqualTo(Money.of(200000L)) },
                { assertThat(order.couponId).isNull() },
            )
        }

        @DisplayName("Redis 쿠폰 중복 선점 시 CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenCouponAlreadyReserved() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon()
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // 첫 주문 성공
            orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            // act: 같은 쿠폰으로 두 번째 주문
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponDoesNotExist() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, 999L, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생하고 Redis 선점이 복원된다.")
        @Test
        fun throwsBadRequest_whenCouponIsExpired() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("결제 실패 시 Redis 쿠폰 선점이 복원된다.")
        @Test
        fun restoresCouponReservation_whenPaymentFails() {
            // arrange
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-fail", status = "FAILED", reason = "잔액 부족"),
            )
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon()
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)
            }

            // assert: 쿠폰 선점 복원됨 → 다시 선점 가능해야 함
            // (결제 실패 후 Redis 쿠폰 키가 삭제되었으므로 PG mock을 성공으로 복원 후 재주문 가능)
            whenever(paymentGateway.requestPayment(any(), any(), any(), any(), any(), any())).thenReturn(
                PaymentGatewayResponse(transactionKey = "txn-retry", status = "PENDING", reason = null),
            )
            orderFacade.placeOrder(userId, items, coupon.id, entryToken = issueEntryToken(userId), cardType = TEST_CARD_TYPE, cardNo = TEST_CARD_NO)

            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            assertThat(orders).hasSize(1)
        }
    }

    @DisplayName("멱등성 키로 중복 주문을 방지할 때,")
    @Nested
    inner class IdempotencyKeyDuplicatePrevention {

        @DisplayName("같은 idempotencyKey로 두 번 주문하면, 주문이 1건만 생성된다.")
        @Test
        fun createsOnlyOneOrder_whenSameIdempotencyKey() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))
            val idempotencyKey = "test-idempotency-key-123"

            // act
            orderFacade.placeOrder(userId, items, null, idempotencyKey, issueEntryToken(userId), TEST_CARD_TYPE, TEST_CARD_NO)
            orderFacade.placeOrder(userId, items, null, idempotencyKey, issueEntryToken(userId), TEST_CARD_TYPE, TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            assertThat(orders).hasSize(1)
        }

        @DisplayName("다른 idempotencyKey로 주문하면, 각각 주문이 생성된다.")
        @Test
        fun createsSeparateOrders_whenDifferentIdempotencyKeys() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            orderFacade.placeOrder(userId, items, null, "key-1", issueEntryToken(userId), TEST_CARD_TYPE, TEST_CARD_NO)
            orderFacade.placeOrder(userId, items, null, "key-2", issueEntryToken(userId), TEST_CARD_TYPE, TEST_CARD_NO)

            // assert
            val orders = orderService.getOrders(userId, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1))
            assertThat(orders).hasSize(2)
        }
    }
}
