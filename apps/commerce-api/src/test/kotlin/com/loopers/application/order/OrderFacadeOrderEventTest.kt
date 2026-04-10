package com.loopers.application.order

import com.loopers.application.event.OrderCompletedEvent
import com.loopers.application.event.OrderCompletedItem
import com.loopers.application.payment.PaymentFacade
import com.loopers.application.queue.QueueFacade
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponIssueService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

@DisplayName("OrderFacade - 주문 이벤트 발행")
class OrderFacadeOrderEventTest {

    private val orderService: OrderService = mockk()
    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val couponIssueService: CouponIssueService = mockk()
    private val couponService: CouponService = mockk()
    private val orderTransactionRunner: OrderTransactionRunner = mockk()
    private val paymentFacade: PaymentFacade = mockk(relaxed = true)
    private val queueFacade: QueueFacade = mockk(relaxed = true)
    private val applicationEventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private val orderFacade = OrderFacade(
        orderTransactionRunner,
        paymentFacade,
        queueFacade,
        orderService,
        productService,
        brandService,
        couponIssueService,
        couponService,
        applicationEventPublisher,
    )

    init {
        every { orderTransactionRunner.runInTransaction<OrderModel>(any()) } answers {
            firstArg<() -> OrderModel>().invoke()
        }
    }

    @DisplayName("주문 생성 시 발행되는 이벤트에 주문 상품 목록이 포함된다")
    @Test
    fun publishesEventWithOrderItems() {
        // arrange
        val product1 = createProduct(id = 1L, price = 25000L, stockQuantity = 10)
        val product2 = createProduct(id = 2L, price = 5000L, stockQuantity = 20)
        val brand = createBrand()
        val items = listOf(
            OrderItemRequest(productId = 1L, quantity = 2),
            OrderItemRequest(productId = 2L, quantity = 3),
        )

        every { productService.findAllByIdsForUpdate(listOf(1L, 2L)) } returns listOf(product1, product2)
        every { brandService.findAllByIds(listOf(10L)) } returns listOf(brand)
        mockOrderCreation()

        val eventSlot = slot<OrderCompletedEvent>()
        every { applicationEventPublisher.publishEvent(capture(eventSlot)) } answers {}

        // act
        orderFacade.createOrder(1L, items)

        // assert
        assertThat(eventSlot.isCaptured).isTrue()
        val event = eventSlot.captured
        assertThat(event.orderItems).hasSize(2)
        assertThat(event.orderItems).containsExactlyInAnyOrder(
            OrderCompletedItem(productId = 1L, quantity = 2),
            OrderCompletedItem(productId = 2L, quantity = 3),
        )
    }

    private fun createProduct(id: Long, price: Long = 25000L, stockQuantity: Int = 100): ProductModel {
        val product = ProductModel(name = "상품$id", price = price, brandId = 10L, stockQuantity = stockQuantity)
        return spyk(product) { every { this@spyk.id } returns id }
    }

    private fun createBrand(): BrandModel {
        val brand = BrandModel(name = "루프팩")
        return spyk(brand) { every { this@spyk.id } returns 10L }
    }

    private fun mockOrderCreation() {
        every {
            orderService.createOrder(userId = any(), orderItems = any(), brandNameResolver = any())
        } answers {
            val orderItems = secondArg<List<Pair<ProductModel, Int>>>()
            val resolver = thirdArg<(Long) -> String>()
            val order = OrderModel(userId = firstArg())
            orderItems.forEach { (prod, qty) ->
                prod.decreaseStock(qty)
                order.addItem(
                    OrderItemModel(
                        order = order,
                        productId = prod.id,
                        productName = prod.name,
                        brandName = resolver(prod.brandId),
                        price = prod.price,
                        quantity = qty,
                    ),
                )
            }
            order
        }
    }
}
