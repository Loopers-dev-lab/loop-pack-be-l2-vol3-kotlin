package com.loopers.application.payment

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
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderPaymentProcessor
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentProcessor
import com.loopers.domain.payment.PaymentReader
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.payment.PgPaymentStatus
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
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime

class PaymentUseCaseTest {

    @Nested
    inner class RequestPayment {
        @Test
        fun `PG가_수락하면_주문은_PAYMENT_PENDING_결제는_PENDING_상태가_된다`() {
            val fixture = createFixture()
            every {
                fixture.paymentGateway.requestPayment(any(), any())
            } returns PaymentGateway.RequestResult.Accepted(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.PENDING,
                reason = null,
            )

            val result = fixture.useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.SAMSUNG,
                    cardNo = "1234-5678-1234-5678",
                ),
            )

            assertThat(result.orderStatus).isEqualTo(OrderStatus.PAYMENT_PENDING.name)
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.PENDING.name)
            assertThat(result.transactionKey).isEqualTo("20250816:TR:9577c5")
            assertThat(result.cardNo).isEqualTo("****-****-****-5678")
        }

        @Test
        fun `타임아웃이면_주문은_PAYMENT_PENDING_결제는_UNKNOWN_상태가_된다`() {
            val fixture = createFixture()
            every {
                fixture.paymentGateway.requestPayment(any(), any())
            } returns PaymentGateway.RequestResult.Unknown("PG 요청 타임아웃 또는 네트워크 오류가 발생했습니다.")

            val result = fixture.useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.KB,
                    cardNo = "2222-3333-4444-5555",
                ),
            )

            assertThat(result.orderStatus).isEqualTo(OrderStatus.PAYMENT_PENDING.name)
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.UNKNOWN.name)
        }

        @Test
        fun `PG_요청이_실패하면_RESERVED_쿠폰을_해제한다`() {
            val fixture = createFixture(
                order = createOrder(couponId = 1L),
                issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED),
            )
            every {
                fixture.paymentGateway.requestPayment(any(), any())
            } returns PaymentGateway.RequestResult.RequestFailed("잔액 부족")

            val result = fixture.useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.KB,
                    cardNo = "2222-3333-4444-5555",
                ),
            )

            assertThat(result.orderStatus).isEqualTo(OrderStatus.PAYMENT_FAILED.name)
            assertThat(result.paymentStatus).isEqualTo(PaymentStatus.REQUEST_FAILED.name)
            assertThat(fixture.issuedCouponStore.findById(1L)?.status).isEqualTo(CouponStatus.AVAILABLE)
            assertThat(fixture.productStore.findById(1L)?.stock?.value).isEqualTo(10)
        }
    }

    @Nested
    inner class SyncPayment {
        @Test
        fun `수동_동기화로_SUCCESS를_반영하면_주문이_PAID가_되고_RESERVED_쿠폰이_USED가_된다`() {
            val fixture = createFixture(
                order = createOrder(couponId = 1L),
                issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED),
            )
            every {
                fixture.paymentGateway.requestPayment(any(), any())
            } returns PaymentGateway.RequestResult.Accepted(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.PENDING,
                reason = null,
            )

            fixture.useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.HYUNDAI,
                    cardNo = "9999-8888-7777-6666",
                ),
            )

            every {
                fixture.paymentGateway.getTransaction(1L, "20250816:TR:9577c5")
            } returns PaymentGateway.LookupResult.Found(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.SUCCESS,
                reason = "정상 승인되었습니다.",
            )

            val synced = fixture.useCase.syncPayment(memberId = 1L, orderId = 1L)

            assertThat(synced.orderStatus).isEqualTo(OrderStatus.PAID.name)
            assertThat(synced.paymentStatus).isEqualTo(PaymentStatus.SUCCESS.name)
            assertThat(fixture.issuedCouponStore.findById(1L)?.status).isEqualTo(CouponStatus.USED)
        }
    }

    @Nested
    inner class HandleCallback {
        @Test
        fun `콜백으로_SUCCESS를_반영하면_주문이_PAID가_되고_RESERVED_쿠폰이_USED가_된다`() {
            val fixture = createFixture(
                order = createOrder(couponId = 1L),
                issuedCoupon = createIssuedCoupon(status = CouponStatus.RESERVED),
            )
            every {
                fixture.paymentGateway.requestPayment(any(), any())
            } returns PaymentGateway.RequestResult.Accepted(
                transactionKey = "20250816:TR:9577c5",
                status = PgPaymentStatus.PENDING,
                reason = null,
            )

            fixture.useCase.requestPayment(
                memberId = 1L,
                command = PaymentUseCase.RequestCommand(
                    orderId = 1L,
                    cardType = CardType.SAMSUNG,
                    cardNo = "1234-5678-1234-5678",
                ),
            )

            fixture.useCase.handleCallback(
                PaymentUseCase.CallbackCommand(
                    transactionKey = "20250816:TR:9577c5",
                    status = PgPaymentStatus.SUCCESS,
                    reason = "정상 승인",
                ),
            )

            assertThat(fixture.orderStore.findById(1L)?.status).isEqualTo(OrderStatus.PAID)
            assertThat(fixture.paymentStore.findLatestByOrderId(1L)?.status).isEqualTo(PaymentStatus.SUCCESS)
            assertThat(fixture.issuedCouponStore.findById(1L)?.status).isEqualTo(CouponStatus.USED)
        }
    }

    private fun createFixture(
        order: Order = createOrder(),
        issuedCoupon: IssuedCoupon? = null,
    ): Fixture {
        val paymentGateway = mockk<PaymentGateway>()
        val orderStore = FakeOrderStore(order)
        val paymentStore = FakePaymentStore()
        val issuedCouponStore = FakeIssuedCouponStore(issuedCoupon)
        val productStore = FakeProductStore()
        val orderReader = OrderReader(orderStore)
        val paymentReader = PaymentReader(paymentStore)

        val useCase = PaymentUseCase(
            orderReader = orderReader,
            orderPaymentProcessor = OrderPaymentProcessor(
                orderReader,
                orderStore,
                ProductStockDeductor(ProductReader(productStore), productStore),
            ),
            paymentReader = paymentReader,
            paymentProcessor = PaymentProcessor(paymentReader, paymentStore),
            issuedCouponProcessor = IssuedCouponProcessor(
                couponReader = CouponReader(FakeCouponStore()),
                issuedCouponReader = IssuedCouponReader(issuedCouponStore),
                issuedCouponRepository = issuedCouponStore,
            ),
            paymentGateway = paymentGateway,
            transactionTemplate = TransactionTemplate(NoOpTransactionManager()),
            applicationEventPublisher = mockk(relaxed = true),
            outboxEventWriter = mockk(relaxed = true),
        )

        return Fixture(
            useCase = useCase,
            paymentGateway = paymentGateway,
            orderStore = orderStore,
            paymentStore = paymentStore,
            issuedCouponStore = issuedCouponStore,
            productStore = productStore,
        )
    }

    private data class Fixture(
        val useCase: PaymentUseCase,
        val paymentGateway: PaymentGateway,
        val orderStore: FakeOrderStore,
        val paymentStore: FakePaymentStore,
        val issuedCouponStore: FakeIssuedCouponStore,
        val productStore: FakeProductStore,
    )

    private class FakeOrderStore(
        private var order: Order,
    ) : OrderRepository {
        override fun save(order: Order): Order {
            this.order = order
            return order
        }

        override fun findById(id: Long): Order? = if (order.id == id) order else null

        override fun findByIdForUpdate(id: Long): Order? = findById(id)
        override fun findAllByMemberId(memberId: Long): List<Order> =
            if (order.memberId == memberId) listOf(order) else emptyList()
    }

    private class FakePaymentStore : PaymentRepository {
        private val payments = linkedMapOf<Long, Payment>()
        private var sequence = 1L

        override fun save(payment: Payment): Payment {
            val persisted = if (payment.id == null) {
                Payment(
                    id = sequence++,
                    orderId = payment.orderId,
                    memberId = payment.memberId,
                    cardType = payment.cardType,
                    cardNo = payment.cardNo,
                    amount = payment.amount,
                    requestedAt = payment.requestedAt,
                    status = payment.status,
                    pgTransactionKey = payment.pgTransactionKey,
                    reason = payment.reason,
                )
            } else {
                payment
            }
            payments[requireNotNull(persisted.id)] = persisted
            return persisted
        }

        override fun findById(id: Long): Payment? = payments[id]

        override fun findLatestByOrderId(orderId: Long): Payment? =
            payments.values.filter { it.orderId == orderId }.maxByOrNull { requireNotNull(it.id) }

        override fun findLatestByOrderId(orderId: Long, memberId: Long): Payment? =
            payments.values
                .filter { it.orderId == orderId && it.memberId == memberId }
                .maxByOrNull { requireNotNull(it.id) }

        override fun findByPgTransactionKey(transactionKey: String): Payment? =
            payments.values.lastOrNull { it.pgTransactionKey == transactionKey }
    }

    private class FakeProductStore : ProductRepository {
        private var product = Product(
            id = 1L,
            brandId = 1L,
            name = ProductName("결제상품"),
            price = ProductPrice(5000L),
            description = ProductDescription("설명"),
            stock = Stock(9),
            status = ProductStatus.SELLING,
        )

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

        override fun deductStock(productId: Long, quantity: Int): Int = 0

        override fun restoreStock(productId: Long, quantity: Int): Int {
            val current = findById(productId) ?: return 0
            current.restoreStock(quantity)
            return 1
        }

        override fun incrementLikeCount(productId: Long): Int = 0

        override fun decrementLikeCount(productId: Long): Int = 0
    }

    private class FakeIssuedCouponStore(
        private var issuedCoupon: IssuedCoupon?,
    ) : IssuedCouponRepository {
        override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
            this.issuedCoupon = issuedCoupon
            return issuedCoupon
        }

        override fun findById(id: Long): IssuedCoupon? = issuedCoupon?.takeIf { it.id == id }

        override fun findByIdForUpdate(id: Long): IssuedCoupon? = findById(id)

        override fun findAllByMemberId(memberId: Long): List<IssuedCoupon> =
            issuedCoupon?.takeIf { it.memberId == memberId }?.let(::listOf) ?: emptyList()

        override fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean =
            issuedCoupon?.couponId == couponId && issuedCoupon?.memberId == memberId

        override fun findAllByCouponId(couponId: Long, pageable: org.springframework.data.domain.Pageable): Page<IssuedCoupon> =
            PageImpl(issuedCoupon?.takeIf { it.couponId == couponId }?.let(::listOf) ?: emptyList())
    }

    private class FakeCouponStore : CouponRepository {
        private val coupon = Coupon(
            id = 1L,
            name = CouponName("테스트 쿠폰"),
            type = CouponType.FIXED,
            discountValue = DiscountValue(3000L),
            minOrderAmount = MinOrderAmount(null),
            expiredAt = ZonedDateTime.now().plusDays(1),
        )

        override fun save(coupon: Coupon): Coupon = coupon

        override fun findById(id: Long): Coupon? = if (coupon.id == id) coupon else null

        override fun findAllByIds(ids: List<Long>): List<Coupon> =
            if (coupon.id in ids) listOf(coupon) else emptyList()

        override fun findAll(pageable: org.springframework.data.domain.Pageable): Page<Coupon> = PageImpl(listOf(coupon))

        override fun tryIncreaseIssuedCount(id: Long): Int = 1
        override fun deleteById(id: Long) = Unit
    }

    private class NoOpTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    private fun createOrder(couponId: Long? = null) = Order(
        id = 1L,
        memberId = 1L,
        orderItems = listOf(
            OrderItem(
                id = 1L,
                productId = 1L,
                productName = "결제상품",
                productPrice = 5000L,
                quantity = 1,
            ),
        ),
        totalPrice = 5000L,
        finalPrice = 5000L,
        couponId = couponId,
        orderedAt = ZonedDateTime.now(),
    )

    private fun createIssuedCoupon(status: CouponStatus) = IssuedCoupon(
        id = 1L,
        couponId = 1L,
        memberId = 1L,
        status = status,
        issuedAt = ZonedDateTime.now(),
    )
}
