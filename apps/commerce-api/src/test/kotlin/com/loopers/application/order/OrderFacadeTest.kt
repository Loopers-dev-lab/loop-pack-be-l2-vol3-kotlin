package com.loopers.application.order

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponIssueModel
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponIssueStatus
import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("OrderFacade")
class OrderFacadeTest {

    private val orderService: OrderService = mockk()
    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val couponService: CouponService = mockk()
    private val orderFacade = OrderFacade(orderService, productService, brandService, couponIssueService, couponService)

    companion object {
        private const val USER_ID = 1L
        private const val BRAND_ID = 10L
        private const val BRAND_NAME = "루프팩"
        private const val PRODUCT_NAME_1 = "감성 티셔츠"
        private const val PRODUCT_NAME_2 = "캔버스백"
    }

    private fun createProduct(
        id: Long = 1L,
        name: String = PRODUCT_NAME_1,
        price: Long = 25000L,
        brandId: Long = BRAND_ID,
        stockQuantity: Int = 100,
    ): ProductModel {
        val product = ProductModel(
            name = name,
            price = price,
            brandId = brandId,
            stockQuantity = stockQuantity,
        )
        return spyk(product) {
            every { this@spyk.id } returns id
        }
    }

    private fun createBrand(
        id: Long = BRAND_ID,
        name: String = BRAND_NAME,
    ): BrandModel {
        val brand = BrandModel(name = name)
        return spyk(brand) {
            every { this@spyk.id } returns id
        }
    }

    private fun createCouponIssue(
        id: Long = 100L,
        couponId: Long = 50L,
        userId: Long = USER_ID,
        status: CouponIssueStatus = CouponIssueStatus.AVAILABLE,
    ): CouponIssueModel {
        val issue = CouponIssueModel(couponId = couponId, userId = userId, status = status)
        return spyk(issue) {
            every { this@spyk.id } returns id
        }
    }

    private fun createCoupon(
        id: Long = 50L,
        name: String = "3000원 할인",
        type: CouponType = CouponType.FIXED,
        value: Long = 3000L,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponModel {
        val coupon = CouponModel(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        return spyk(coupon) {
            every { this@spyk.id } returns id
        }
    }

    private fun mockOrderCreation() {
        every {
            orderService.createOrder(
                userId = USER_ID,
                orderItems = any(),
                brandNameResolver = any(),
            )
        } answers {
            val orderItems = secondArg<List<Pair<ProductModel, Int>>>()
            val resolver = thirdArg<(Long) -> String>()
            val order = OrderModel(userId = USER_ID)
            orderItems.forEach { (prod, qty) ->
                prod.decreaseStock(qty)
                val item = OrderItemModel(
                    order = order,
                    productId = prod.id,
                    productName = prod.name,
                    brandName = resolver(prod.brandId),
                    price = prod.price,
                    quantity = qty,
                )
                order.addItem(item)
            }
            order
        }
    }

    @DisplayName("createOrder - 쿠폰 없이 주문")
    @Nested
    inner class CreateOrderWithoutCoupon {
        @DisplayName("정상적인 단일 상품 주문이 생성된다")
        @Test
        fun createsOrder_whenSingleProductWithSufficientStock() {
            // arrange
            val product = createProduct(id = 1L, stockQuantity = 10)
            val brand = createBrand()
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 3))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            mockOrderCreation()

            // act
            val result = orderFacade.createOrder(USER_ID, items)

            // assert
            assertThat(result.userId).isEqualTo(USER_ID)
            assertThat(result.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(result.orderItems).hasSize(1)
            assertThat(result.originalTotalAmount).isEqualTo(25000L * 3)
            assertThat(result.discountAmount).isEqualTo(0L)
            assertThat(result.totalAmount).isEqualTo(25000L * 3)
            assertThat(result.couponIssueId).isNull()
        }

        @DisplayName("다중 상품 주문이 정상적으로 생성되고 총 금액이 정확하다")
        @Test
        fun createsOrder_whenMultipleProductsWithSufficientStock() {
            // arrange
            val product1 = createProduct(id = 1L, name = PRODUCT_NAME_1, price = 25000L, stockQuantity = 10)
            val product2 = createProduct(id = 2L, name = PRODUCT_NAME_2, price = 5000L, stockQuantity = 20)
            val brand = createBrand()
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 2L, quantity = 1),
            )

            every { productService.findAllByIdsForUpdate(listOf(1L, 2L)) } returns listOf(product1, product2)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            mockOrderCreation()

            // act
            val result = orderFacade.createOrder(USER_ID, items)

            // assert
            assertThat(result.orderItems).hasSize(2)
            assertThat(result.totalAmount).isEqualTo(25000L * 2 + 5000L * 1)
        }

        @DisplayName("중복 상품 ID가 포함되면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenDuplicateProductIds() {
            // arrange
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 1L, quantity = 3),
            )

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            verify(exactly = 0) { productService.findAllByIdsForUpdate(any()) }
        }

        @DisplayName("존재하지 않는 상품이 포함되면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            val product = createProduct(id = 1L)
            val items = listOf(
                OrderItemRequest(productId = 1L, quantity = 2),
                OrderItemRequest(productId = 999L, quantity = 1),
            )

            every { productService.findAllByIdsForUpdate(listOf(1L, 999L)) } returns listOf(product)

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }

        @DisplayName("재고 부족 시 CoreException이 발생한다")
        @Test
        fun throwsBadRequest_whenInsufficientStock() {
            // arrange
            val product = createProduct(id = 1L, stockQuantity = 3)
            val brand = createBrand()
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 5))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every {
                orderService.createOrder(
                    userId = USER_ID,
                    orderItems = any(),
                    brandNameResolver = any(),
                )
            } throws CoreException(
                ErrorType.BAD_REQUEST,
                "상품의 재고가 부족합니다.",
            )

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("재고가 부족합니다")
        }
    }

    @DisplayName("createOrder - 쿠폰 적용 주문")
    @Nested
    inner class CreateOrderWithCoupon {
        @DisplayName("정액 쿠폰을 적용하면 할인 금액이 반영된다")
        @Test
        fun appliesFixedCouponDiscount() {
            // arrange
            val product = createProduct(id = 1L, price = 25000L, stockQuantity = 10)
            val brand = createBrand()
            val couponIssue = createCouponIssue(id = 100L, couponId = 50L, userId = USER_ID)
            val coupon = createCoupon(id = 50L, type = CouponType.FIXED, value = 3000L)
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 2))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every { couponIssueService.findById(100L) } returns couponIssue
            every { couponIssueService.findByIdForUpdate(100L) } returns couponIssue
            every { couponService.findById(50L) } returns coupon
            mockOrderCreation()

            // act
            val result = orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)

            // assert
            assertThat(result.originalTotalAmount).isEqualTo(50000L)
            assertThat(result.discountAmount).isEqualTo(3000L)
            assertThat(result.totalAmount).isEqualTo(47000L)
            assertThat(result.couponIssueId).isEqualTo(100L)
        }

        @DisplayName("정률 쿠폰을 적용하면 비율 할인이 반영된다")
        @Test
        fun appliesRateCouponDiscount() {
            // arrange
            val product = createProduct(id = 1L, price = 50000L, stockQuantity = 10)
            val brand = createBrand()
            val couponIssue = createCouponIssue(id = 100L, couponId = 50L, userId = USER_ID)
            val coupon = createCoupon(id = 50L, type = CouponType.RATE, value = 10L)
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 1))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { brandService.findAllByIds(listOf(BRAND_ID)) } returns listOf(brand)
            every { couponIssueService.findById(100L) } returns couponIssue
            every { couponIssueService.findByIdForUpdate(100L) } returns couponIssue
            every { couponService.findById(50L) } returns coupon
            mockOrderCreation()

            // act
            val result = orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)

            // assert
            assertThat(result.originalTotalAmount).isEqualTo(50000L)
            assertThat(result.discountAmount).isEqualTo(5000L)
            assertThat(result.totalAmount).isEqualTo(45000L)
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            val product = createProduct(id = 1L)
            val couponIssue = createCouponIssue(id = 100L, userId = USER_ID, status = CouponIssueStatus.USED)
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 1))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { couponIssueService.findById(100L) } returns couponIssue

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("만료된 쿠폰으로 주문하면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenCouponExpired() {
            // arrange
            val product = createProduct(id = 1L)
            val couponIssue = createCouponIssue(id = 100L, userId = USER_ID)
            val coupon = createCoupon(id = 50L, expiredAt = ZonedDateTime.now().minusDays(1))
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 1))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { couponIssueService.findById(100L) } returns couponIssue
            every { couponService.findById(50L) } returns coupon

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("타 유저의 쿠폰으로 주문하면 BAD_REQUEST 예외가 발생한다")
        @Test
        fun throwsBadRequest_whenCouponBelongsToOtherUser() {
            // arrange
            val product = createProduct(id = 1L)
            val otherUserId = 999L
            val couponIssue = createCouponIssue(id = 100L, userId = otherUserId)
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 1))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { couponIssueService.findById(100L) } returns couponIssue

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 쿠폰으로 주문하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFound_whenCouponDoesNotExist() {
            // arrange
            val product = createProduct(id = 1L)
            val items = listOf(OrderItemRequest(productId = 1L, quantity = 1))

            every { productService.findAllByIdsForUpdate(listOf(1L)) } returns listOf(product)
            every { couponIssueService.findById(100L) } throws CoreException(
                ErrorType.NOT_FOUND,
                "존재하지 않는 발급 쿠폰입니다: 100",
            )

            // act & assert
            assertThatThrownBy {
                orderFacade.createOrder(USER_ID, items, couponIssueId = 100L)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)
        }
    }
}
