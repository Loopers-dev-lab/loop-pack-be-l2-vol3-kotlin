package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.fixture.FakeBrandRepository
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponException
import com.loopers.domain.coupon.CouponName
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.fixture.FakeCouponRepository
import com.loopers.domain.coupon.fixture.FakeUserCouponRepository
import com.loopers.domain.order.fixture.FakeOrderRepository
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductException
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.Stock
import com.loopers.domain.product.fixture.FakeProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class CreateOrderUseCaseTest {

    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var userCouponRepository: FakeUserCouponRepository
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @BeforeEach
    fun setUp() {
        orderRepository = FakeOrderRepository()
        productRepository = FakeProductRepository()
        brandRepository = FakeBrandRepository()
        couponRepository = FakeCouponRepository()
        userCouponRepository = FakeUserCouponRepository()
        createOrderUseCase = CreateOrderUseCase(
            orderRepository, productRepository, brandRepository, userCouponRepository,
        )
    }

    @Test
    fun `정상 주문 시 주문이 생성되고 재고가 차감되어야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = ORDER_QUANTITY)),
        )

        val orderId = createOrderUseCase.create(USER_ID, command)

        assertThat(orderId).isPositive()
        val product = productRepository.findById(productId)!!
        assertThat(product.stock.quantity).isEqualTo(STOCK - ORDER_QUANTITY)
    }

    @Test
    fun `재고가 부족한 경우 ProductException이 발생해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = STOCK + 1)),
        )

        assertThatThrownBy { createOrderUseCase.create(USER_ID, command) }
            .isInstanceOf(ProductException::class.java)
    }

    @Test
    fun `삭제된 상품 주문 시 ProductException이 발생해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val deletedProduct = createProduct(brandId).delete()
        val productId = productRepository.save(deletedProduct)
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = ORDER_QUANTITY)),
        )

        assertThatThrownBy { createOrderUseCase.create(USER_ID, command) }
            .isInstanceOf(ProductException::class.java)
    }

    @Test
    fun `존재하지 않는 상품 주문 시 CoreException이 발생해야 한다`() {
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = 999L, quantity = ORDER_QUANTITY)),
        )

        assertThatThrownBy { createOrderUseCase.create(USER_ID, command) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND)
    }

    @Test
    fun `쿠폰 적용 주문 시 할인이 적용되어야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))
        val couponId = couponRepository.save(createCoupon())
        val userCouponId = userCouponRepository.save(
            UserCoupon.issue(couponRepository.findById(couponId)!!, USER_ID),
        )
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = ORDER_QUANTITY)),
            couponId = userCouponId,
        )

        val orderId = createOrderUseCase.create(USER_ID, command)

        val order = orderRepository.findById(orderId)!!
        assertThat(order.refUserCouponId).isEqualTo(userCouponId)
        assertThat(order.discountAmount.amount).isEqualTo(DISCOUNT_VALUE)
        assertThat(order.totalAmount.amount).isEqualTo(PRICE * ORDER_QUANTITY - DISCOUNT_VALUE)
    }

    @Test
    fun `쿠폰 적용 주문 시 UserCoupon이 USED 상태가 되어야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))
        val couponId = couponRepository.save(createCoupon())
        val userCouponId = userCouponRepository.save(
            UserCoupon.issue(couponRepository.findById(couponId)!!, USER_ID),
        )
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = ORDER_QUANTITY)),
            couponId = userCouponId,
        )

        createOrderUseCase.create(USER_ID, command)

        val userCoupon = userCouponRepository.findById(userCouponId)!!
        assertThat(userCoupon.status).isEqualTo(CouponStatus.USED)
    }

    @Test
    fun `타인의 쿠폰으로 주문하면 CouponException이 발생해야 한다`() {
        val brandId = brandRepository.save(createBrand())
        val productId = productRepository.save(createProduct(brandId))
        val couponId = couponRepository.save(createCoupon())
        val otherUserCouponId = userCouponRepository.save(
            UserCoupon.issue(couponRepository.findById(couponId)!!, OTHER_USER_ID),
        )
        val command = CreateOrderCommand(
            items = listOf(OrderItemCommand(productId = productId, quantity = ORDER_QUANTITY)),
            couponId = otherUserCouponId,
        )

        assertThatThrownBy { createOrderUseCase.create(USER_ID, command) }
            .isInstanceOf(CouponException::class.java)
    }

    private fun createCoupon() = Coupon.create(
        name = CouponName("테스트쿠폰"),
        discountType = DiscountType.FIXED,
        discountValue = DISCOUNT_VALUE,
        minOrderAmount = Money(0),
        maxIssueCount = 100,
        expiredAt = ZonedDateTime.now().plusDays(30),
    )

    private fun createBrand() = Brand.create(
        name = BrandName(BRAND_NAME),
        description = BRAND_DESCRIPTION,
        logoUrl = BRAND_LOGO_URL,
    )

    private fun createProduct(brandId: Long) = Product.create(
        brandId = brandId,
        name = ProductName(PRODUCT_NAME),
        description = PRODUCT_DESCRIPTION,
        price = Money(PRICE),
        stock = Stock(STOCK),
        thumbnailUrl = THUMBNAIL_URL,
        images = emptyList(),
    )

    companion object {
        private const val USER_ID = 1L
        private const val OTHER_USER_ID = 2L
        private const val BRAND_NAME = "테스트브랜드"
        private const val BRAND_DESCRIPTION = "브랜드 설명"
        private const val BRAND_LOGO_URL = "https://example.com/logo.png"
        private const val PRODUCT_NAME = "테스트상품"
        private const val PRODUCT_DESCRIPTION = "상품 설명"
        private const val PRICE = 10000L
        private const val STOCK = 100
        private const val ORDER_QUANTITY = 2
        private const val THUMBNAIL_URL = "https://example.com/thumb.png"
        private const val DISCOUNT_VALUE = 3000L
    }
}
