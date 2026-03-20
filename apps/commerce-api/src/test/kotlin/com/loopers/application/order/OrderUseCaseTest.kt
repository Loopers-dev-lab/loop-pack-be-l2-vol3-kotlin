package com.loopers.application.order

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponProcessor
import com.loopers.domain.coupon.IssuedCouponReader
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.coupon.vo.CouponName
import com.loopers.domain.coupon.vo.DiscountValue
import com.loopers.domain.coupon.vo.MinOrderAmount
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderCanceller
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRegister
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductReader
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.ProductStockDeductor
import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.ZonedDateTime

class OrderUseCaseTest {

    @Test
    fun `주문_생성시_쿠폰은_RESERVED_상태가_된다`() {
        val fixture = createFixture()

        val result = fixture.useCase.createOrder(
            memberId = 1L,
            command = OrderUseCase.CreateOrderCommand(
                items = listOf(OrderUseCase.OrderItemRequest(productId = 1L, quantity = 2)),
                couponId = 1L,
            ),
        )

        assertThat(result.discountAmount).isEqualTo(3000L)
        assertThat(result.finalPrice).isEqualTo(7000L)
        assertThat(fixture.issuedCouponStore.findById(1L)?.status).isEqualTo(CouponStatus.RESERVED)
    }

    @Test
    fun `결제_전_주문_취소시_RESERVED_쿠폰은_해제된다`() {
        val fixture = createFixture()

        val order = fixture.useCase.createOrder(
            memberId = 1L,
            command = OrderUseCase.CreateOrderCommand(
                items = listOf(OrderUseCase.OrderItemRequest(productId = 1L, quantity = 1)),
                couponId = 1L,
            ),
        )

        fixture.useCase.cancel(order.id, memberId = 1L)

        assertThat(fixture.issuedCouponStore.findById(1L)?.status).isEqualTo(CouponStatus.AVAILABLE)
    }

    @Test
    fun `결제_실패후_주문_취소시_재고는_다시_복구하지_않는다`() {
        val fixture = createFixture(
            product = Product(
                id = 1L,
                brandId = 1L,
                name = ProductName("테스트 상품"),
                price = ProductPrice(5000L),
                description = ProductDescription("설명"),
                stock = Stock(10),
                status = ProductStatus.SELLING,
            ),
            order = Order(
                id = 1L,
                memberId = 1L,
                orderItems = listOf(
                    com.loopers.domain.order.OrderItem(
                        productId = 1L,
                        productName = "테스트 상품",
                        productPrice = 5000L,
                        quantity = 1,
                    ),
                ),
                totalPrice = 5000L,
                finalPrice = 5000L,
                couponId = 1L,
                orderedAt = ZonedDateTime.now(),
                status = OrderStatus.PAYMENT_FAILED,
            ),
            issuedCoupon = IssuedCoupon(
                id = 1L,
                couponId = 1L,
                memberId = 1L,
                status = CouponStatus.AVAILABLE,
                issuedAt = ZonedDateTime.now(),
            ),
        )

        fixture.useCase.cancel(1L, memberId = 1L)

        assertThat(fixture.productStore.findById(1L)?.stock?.value).isEqualTo(10)
    }

    private fun createFixture(
        product: Product = Product(
            id = 1L,
            brandId = 1L,
            name = ProductName("테스트 상품"),
            price = ProductPrice(5000L),
            description = ProductDescription("설명"),
            stock = Stock(10),
            status = ProductStatus.SELLING,
        ),
        order: Order? = null,
        issuedCoupon: IssuedCoupon = IssuedCoupon(
            id = 1L,
            couponId = 1L,
            memberId = 1L,
            status = CouponStatus.AVAILABLE,
            issuedAt = ZonedDateTime.now(),
        ),
    ): Fixture {
        val productStore = FakeProductStore(product)
        val couponStore = FakeCouponStore(
            Coupon(
                id = 1L,
                name = CouponName("3천원 할인"),
                type = CouponType.FIXED,
                discountValue = DiscountValue(3000L),
                minOrderAmount = MinOrderAmount(null),
                expiredAt = ZonedDateTime.now().plusDays(7),
            ),
        )
        val issuedCouponStore = FakeIssuedCouponStore(issuedCoupon)
        val orderStore = FakeOrderStore(order)
        val orderReader = OrderReader(orderStore)

        val useCase = OrderUseCase(
            orderRegister = OrderRegister(orderStore),
            orderReader = orderReader,
            orderCanceller = OrderCanceller(orderReader, orderStore),
            productStockDeductor = ProductStockDeductor(ProductReader(productStore), productStore),
            issuedCouponProcessor = IssuedCouponProcessor(
                couponReader = CouponReader(couponStore),
                issuedCouponReader = IssuedCouponReader(issuedCouponStore),
                issuedCouponRepository = issuedCouponStore,
            ),
        )

        return Fixture(useCase, issuedCouponStore, productStore)
    }

    private data class Fixture(
        val useCase: OrderUseCase,
        val issuedCouponStore: FakeIssuedCouponStore,
        val productStore: FakeProductStore,
    )

    private class FakeOrderStore : com.loopers.domain.order.OrderRepository {
        private val orders = linkedMapOf<Long, Order>()
        private var sequence = 1L

        constructor(initialOrder: Order? = null) {
            if (initialOrder != null) {
                orders[requireNotNull(initialOrder.id)] = initialOrder
                sequence = initialOrder.id + 1
            }
        }

        override fun save(order: Order): Order {
            val persisted = if (order.id == null) {
                Order(
                    id = sequence++,
                    memberId = order.memberId,
                    orderItems = order.orderItems,
                    totalPrice = order.totalPrice,
                    discountAmount = order.discountAmount,
                    finalPrice = order.finalPrice,
                    couponId = order.couponId,
                    orderedAt = order.orderedAt,
                    status = order.status,
                )
            } else {
                order
            }
            orders[requireNotNull(persisted.id)] = persisted
            return persisted
        }

        override fun findById(id: Long): Order? = orders[id]

        override fun findByIdForUpdate(id: Long): Order? = findById(id)

        override fun findAllByMemberId(memberId: Long): List<Order> =
            orders.values.filter { it.memberId == memberId }
    }

    private class FakeProductStore(
        private var product: Product,
    ) : ProductRepository {
        override fun save(product: Product): Product {
            this.product = product
            return product
        }

        override fun findById(id: Long): Product? = if (product.id == id) product else null

        override fun findAll(): List<Product> = listOf(product)

        override fun findAll(sortType: ProductSortType, brandId: Long?): List<Product> = listOf(product)

        override fun findAllByBrandId(brandId: Long): List<Product> =
            if (product.brandId == brandId) listOf(product) else emptyList()

        override fun findAllByIds(ids: List<Long>): List<Product> =
            if (product.id in ids) listOf(product) else emptyList()

        override fun existsByBrandIdAndStatus(brandId: Long, status: ProductStatus): Boolean =
            product.brandId == brandId && product.status == status

        override fun deductStock(productId: Long, quantity: Int): Int {
            val current = findById(productId) ?: return 0
            if (current.stock.value < quantity) return 0
            current.deductStock(quantity)
            return 1
        }

        override fun restoreStock(productId: Long, quantity: Int): Int {
            val current = findById(productId) ?: return 0
            current.restoreStock(quantity)
            return 1
        }

        override fun incrementLikeCount(productId: Long): Int = 0

        override fun decrementLikeCount(productId: Long): Int = 0
    }

    private class FakeCouponStore(
        private val coupon: Coupon,
    ) : CouponRepository {
        override fun save(coupon: Coupon): Coupon = coupon

        override fun findById(id: Long): Coupon? = if (coupon.id == id) coupon else null

        override fun findAllByIds(ids: List<Long>): List<Coupon> =
            if (coupon.id in ids) listOf(coupon) else emptyList()

        override fun findAll(pageable: org.springframework.data.domain.Pageable): Page<Coupon> =
            PageImpl(listOf(coupon))

        override fun deleteById(id: Long) = Unit
    }

    private class FakeIssuedCouponStore(
        private var issuedCoupon: IssuedCoupon,
    ) : IssuedCouponRepository {
        override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
            this.issuedCoupon = issuedCoupon
            return issuedCoupon
        }

        override fun findById(id: Long): IssuedCoupon? = if (issuedCoupon.id == id) issuedCoupon else null

        override fun findByIdForUpdate(id: Long): IssuedCoupon? = findById(id)

        override fun findAllByMemberId(memberId: Long): List<IssuedCoupon> =
            if (issuedCoupon.memberId == memberId) listOf(issuedCoupon) else emptyList()

        override fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean =
            issuedCoupon.couponId == couponId && issuedCoupon.memberId == memberId

        override fun findAllByCouponId(couponId: Long, pageable: org.springframework.data.domain.Pageable): Page<IssuedCoupon> =
            PageImpl(if (issuedCoupon.couponId == couponId) listOf(issuedCoupon) else emptyList())
    }
}
