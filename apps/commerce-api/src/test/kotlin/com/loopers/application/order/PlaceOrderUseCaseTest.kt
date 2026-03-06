package com.loopers.application.order

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.FakeCouponRepository
import com.loopers.domain.coupon.FakeIssuedCouponRepository
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.domain.coupon.service.CouponValidator
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class PlaceOrderUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var issuedCouponRepository: FakeIssuedCouponRepository
    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        couponRepository = FakeCouponRepository()
        issuedCouponRepository = FakeIssuedCouponRepository()
        placeOrderUseCase = PlaceOrderUseCase(productRepository, orderRepository, orderItemRepository, couponRepository, issuedCouponRepository, CouponValidator())
    }

    private fun createProduct(
        price: BigDecimal = BigDecimal("10000"),
        stock: Int = 100,
    ): Product {
        return productRepository.save(
            Product(
                refBrandId = BrandId(1),
                name = "에어맥스 90",
                price = Money(price),
                stock = Stock(stock),
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("정상 주문이 생성되고 재고 차감이 수행된다")
        fun execute_success() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.id).isNotEqualTo(0L)
            assertThat(orderInfo.status).isEqualTo("CREATED")
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.items).hasSize(1)

            // 재고 차감 확인
            val updatedProduct = productRepository.findById(product.id)!!
            assertThat(updatedProduct.stock.value).isEqualTo(98)
        }

        @Test
        @DisplayName("재고가 부족하면 CoreException이 발생한다")
        fun execute_insufficientStock_throwsException() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 2)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 5)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("삭제된 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun execute_deletedProduct_throwsException() {
            // arrange
            val product = createProduct()
            product.delete()
            productRepository.save(product)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 항목이 비어있으면 BAD_REQUEST 예외가 발생한다")
        fun execute_emptyItems_throwsBadRequest() {
            // arrange
            val command = PlaceOrderCommand(items = emptyList())

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 수량이 0 이하이면 BAD_REQUEST 예외가 발생한다")
        fun execute_zeroQuantity_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 0)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("상품 가격이 소수점 0.5 이상일 때 총 금액이 정상 계산된다")
        fun execute_fractionalPrice_calculatesCorrectTotal() {
            // arrange -- 1000.50원 x 1개 = 1000.50원
            val product = createProduct(price = BigDecimal("1000.50"), stock = 10)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("1000.50"))
        }

        @Test
        @DisplayName("여러 상품을 포함한 주문이 정상 생성된다")
        fun execute_multipleItems_success() {
            // arrange
            val product1 = productRepository.save(
                Product(
                    refBrandId = BrandId(1),
                    name = "상품1",
                    price = Money(BigDecimal("10000")),
                    stock = Stock(50),
                ),
            )
            val product2 = productRepository.save(
                Product(
                    refBrandId = BrandId(1),
                    name = "상품2",
                    price = Money(BigDecimal("20000")),
                    stock = Stock(50),
                ),
            )
            val command = PlaceOrderCommand(
                items = listOf(
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product1.id.value, quantity = 2),
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product2.id.value, quantity = 3),
                ),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.items).hasSize(2)
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("80000"))

            val p1 = productRepository.findById(product1.id)!!
            val p2 = productRepository.findById(product2.id)!!
            assertThat(p1.stock.value).isEqualTo(48)
            assertThat(p2.stock.value).isEqualTo(47)
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun execute_nonExistentProduct_throwsBadRequest() {
            // arrange
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = 999L, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("중복 상품 ID가 포함된 주문 시 BAD_REQUEST 예외가 발생한다")
        fun execute_duplicateProductId_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2),
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 3),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("중복된 상품")
        }

        @Test
        @DisplayName("쿠폰 미적용 주문 시 originalPrice와 totalPrice가 동일하다")
        fun execute_withoutCoupon_originalEqualsTotal() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.originalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.discountAmount).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.couponId).isNull()
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 주문 시")
    inner class ExecuteWithCoupon {

        private fun createFixedCoupon(
            value: Long = 3000,
            minOrderAmount: Money? = null,
        ): Coupon {
            return couponRepository.save(
                Coupon(
                    name = "정액 할인 쿠폰",
                    type = Coupon.CouponType.FIXED,
                    value = value,
                    minOrderAmount = minOrderAmount,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
        }

        private fun createRateCoupon(
            value: Long = 10,
            maxDiscount: Money? = null,
        ): Coupon {
            return couponRepository.save(
                Coupon(
                    name = "정률 할인 쿠폰",
                    type = Coupon.CouponType.RATE,
                    value = value,
                    maxDiscount = maxDiscount,
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
        }

        private fun issueToUser(coupon: Coupon, userId: Long): IssuedCoupon {
            return issuedCouponRepository.save(
                IssuedCoupon(
                    refCouponId = coupon.id,
                    refUserId = UserId(userId),
                    createdAt = ZonedDateTime.now(),
                ),
            )
        }

        @Test
        @DisplayName("FIXED 쿠폰 적용 시 할인 금액이 반영된 totalPrice가 계산된다")
        fun execute_withFixedCoupon_discountApplied() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createFixedCoupon(value = 3000)
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.originalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.discountAmount).isEqualByComparingTo(BigDecimal("3000"))
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("17000"))
            assertThat(orderInfo.couponId).isEqualTo(coupon.id.value)
        }

        @Test
        @DisplayName("RATE 쿠폰 적용 시 할인 금액이 반영된 totalPrice가 계산된다")
        fun execute_withRateCoupon_discountApplied() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createRateCoupon(value = 10) // 10%
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.originalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.discountAmount).isEqualByComparingTo(BigDecimal("2000"))
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("18000"))
            assertThat(orderInfo.couponId).isEqualTo(coupon.id.value)
        }

        @Test
        @DisplayName("존재하지 않는 발급 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_nonExistentIssuedCoupon_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = 999L,
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("타인 소유 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_otherUserCoupon_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createFixedCoupon(value = 3000)
            val issuedCoupon = issueToUser(coupon, 2L) // 다른 사용자 소유
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("이미 사용된 쿠폰이면 BAD_REQUEST 예외가 발생한다")
        fun execute_alreadyUsedCoupon_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createFixedCoupon(value = 3000)
            val issuedCoupon = issueToUser(coupon, 1L)
            issuedCoupon.use() // 이미 사용 처리
            issuedCouponRepository.save(issuedCoupon)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("최소 주문 금액 미충족 시 BAD_REQUEST 예외가 발생한다")
        fun execute_minOrderAmountNotMet_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("5000"), stock = 100)
            val coupon = createFixedCoupon(value = 3000, minOrderAmount = Money(BigDecimal("20000")))
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("만료된 쿠폰으로 주문 시 BAD_REQUEST 예외가 발생한다")
        fun execute_expiredCoupon_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val expiredCoupon = couponRepository.save(
                Coupon(
                    name = "만료 쿠폰",
                    type = Coupon.CouponType.FIXED,
                    value = 1000,
                    expiredAt = ZonedDateTime.now().minusDays(1),
                ),
            )
            val issuedCoupon = issueToUser(expiredCoupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("RATE 쿠폰의 maxDiscount cap이 적용된다")
        fun execute_rateCouponWithMaxDiscount_capApplied() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createRateCoupon(value = 50, maxDiscount = Money(BigDecimal("2000"))) // 50%, cap=2000
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert -- 10000 * 50% = 5000 이지만 cap=2000 적용
            assertThat(orderInfo.discountAmount).isEqualByComparingTo(BigDecimal("2000"))
        }

        @Test
        @DisplayName("FIXED 쿠폰이 주문금액을 초과하면 주문금액으로 cap된다")
        fun execute_fixedCouponExceedsOrderAmount_capToOrderAmount() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createFixedCoupon(value = 30000) // 쿠폰=30000 > 주문=10000
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert -- 쿠폰 30000원이지만 주문금액 10000원으로 cap
            assertThat(orderInfo.discountAmount).isEqualByComparingTo(BigDecimal("10000"))
        }

        @Test
        @DisplayName("쿠폰 사용 후 IssuedCoupon의 상태가 USED로 변경된다")
        fun execute_withCoupon_issuedCouponStatusBecomesUsed() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val coupon = createFixedCoupon(value = 3000)
            val issuedCoupon = issueToUser(coupon, 1L)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
                issuedCouponId = issuedCoupon.id,
            )

            // act
            placeOrderUseCase.execute(1L, command)

            // assert
            val updated = issuedCouponRepository.findById(issuedCoupon.id)!!
            assertThat(updated.status).isEqualTo(IssuedCoupon.CouponStatus.USED)
        }
    }
}
