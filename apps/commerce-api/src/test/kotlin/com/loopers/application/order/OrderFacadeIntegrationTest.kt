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
import com.loopers.domain.coupon.IssuedCouponStatus
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
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
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(brand: Brand, name: String = "에어맥스", price: Money = Money.of(100000L)): Product {
        return productRepository.save(
            Product(name = name, description = "러닝화", price = price, likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
        )
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

    @DisplayName("쿠폰을 적용하여 주문할 때,")
    @Nested
    inner class PlaceOrderWithCoupon {

        @DisplayName("정액 할인 쿠폰을 적용하면, 할인된 금액으로 주문이 생성된다.")
        @Test
        fun createsOrderWithFixedDiscount() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand, price = Money.of(100000L))
            val coupon = createCoupon(discountType = DiscountType.FIXED_AMOUNT, discountValue = 5000L)
            issueCoupon(coupon.id, userId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(2)))

            // act
            orderFacade.placeOrder(userId, items, coupon.id)

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

        @DisplayName("정률 할인 쿠폰을 적용하면, 비율만큼 할인된 금액으로 주문이 생성된다.")
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
            orderFacade.placeOrder(userId, items, coupon.id)

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
            orderFacade.placeOrder(userId, items, null)

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

        @DisplayName("주문 성공 시, 쿠폰이 USED 상태로 변경된다.")
        @Test
        fun marksCouponAsUsed_whenOrderSucceeds() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon()
            issueCoupon(coupon.id, userId)
            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            orderFacade.placeOrder(userId, items, coupon.id)

            // assert
            val issuedCoupons = issuedCouponRepository.findByUserId(userId)
            val issuedCoupon = issuedCoupons.first { it.couponId == coupon.id }
            assertThat(issuedCoupon.status(coupon.expiresAt)).isEqualTo(IssuedCouponStatus.USED)
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
                orderFacade.placeOrder(userId, items, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("다른 사용자의 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponBelongsToOtherUser() {
            // arrange
            val userId = 1L
            val otherUserId = 2L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon()
            issueCoupon(coupon.id, otherUserId)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, coupon.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 사용된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            val userId = 1L
            val brand = createBrand()
            val product = createProduct(brand)
            val coupon = createCoupon()
            val issuedCoupon = issueCoupon(coupon.id, userId)
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)

            val items = listOf(OrderPlaceCommand(productId = product.id, quantity = Quantity.of(1)))

            // act
            val exception = assertThrows<CoreException> {
                orderFacade.placeOrder(userId, items, coupon.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
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
                orderFacade.placeOrder(userId, items, coupon.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
