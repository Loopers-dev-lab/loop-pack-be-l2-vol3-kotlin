package com.loopers.application.handler

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.BrandService
import com.loopers.application.brand.FakeBrandRepository
import com.loopers.application.like.FakeProductLikeRepository
import com.loopers.application.like.LikeService
import com.loopers.application.order.FakeOrderRepository
import com.loopers.application.order.OrderCommand
import com.loopers.application.order.OrderService
import com.loopers.application.product.FakeProductRepository
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.ProductService
import com.loopers.domain.common.event.BrandCreatedEvent
import com.loopers.domain.common.event.BrandDeletedEvent
import com.loopers.domain.common.event.BrandUpdatedEvent
import com.loopers.domain.common.event.LikeCancelledEvent
import com.loopers.domain.common.event.LikeCreatedEvent
import com.loopers.domain.common.event.OrderCreatedEvent
import com.loopers.domain.common.event.OrderPaidEvent
import com.loopers.domain.common.event.ProductCreatedEvent
import com.loopers.domain.common.event.ProductDeletedEvent
import com.loopers.domain.common.event.ProductUpdatedEvent
import com.loopers.domain.common.event.StockDeductedEvent
import com.loopers.domain.common.event.StockRestoredEvent
import com.loopers.domain.order.OrderStatus
import com.loopers.utils.FakeEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("도메인 이벤트 발행 테스트")
class EventPublishingTest {

    @DisplayName("브랜드 이벤트 발행")
    @Nested
    inner class BrandEventPublishing {
        private lateinit var brandService: BrandService
        private lateinit var eventPublisher: FakeEventPublisher

        @BeforeEach
        fun setUp() {
            eventPublisher = FakeEventPublisher()
            brandService = BrandService(FakeBrandRepository(), eventPublisher)
        }

        @Test
        fun `브랜드 생성 시 BrandCreatedEvent가 발행된다`() {
            // act
            val brand = brandService.createBrand(
                BrandCommand.Create(name = "루퍼스", description = "설명", imageUrl = "https://img.com/a.jpg"),
            )

            // assert
            assertThat(eventPublisher.hasEvent<BrandCreatedEvent>()).isTrue()
            val event = eventPublisher.findEvent<BrandCreatedEvent>()!!
            assertThat(event.brandId).isEqualTo(brand.id)
        }

        @Test
        fun `브랜드 수정 시 BrandUpdatedEvent가 발행된다`() {
            // arrange
            val brand = brandService.createBrand(
                BrandCommand.Create(name = "루퍼스", description = "설명", imageUrl = "https://img.com/a.jpg"),
            )
            eventPublisher.clear()

            // act
            brandService.updateBrand(brand.id, BrandCommand.Update(name = "새이름", description = "새설명", imageUrl = "https://img.com/b.jpg"))

            // assert
            assertThat(eventPublisher.hasEvent<BrandUpdatedEvent>()).isTrue()
            assertThat(eventPublisher.findEvent<BrandUpdatedEvent>()!!.brandId).isEqualTo(brand.id)
        }

        @Test
        fun `브랜드 삭제 시 BrandDeletedEvent가 발행된다`() {
            // arrange
            val brand = brandService.createBrand(
                BrandCommand.Create(name = "루퍼스", description = "설명", imageUrl = "https://img.com/a.jpg"),
            )
            eventPublisher.clear()

            // act
            brandService.deleteBrand(brand.id)

            // assert
            assertThat(eventPublisher.hasEvent<BrandDeletedEvent>()).isTrue()
            assertThat(eventPublisher.findEvent<BrandDeletedEvent>()!!.brandId).isEqualTo(brand.id)
        }
    }

    @DisplayName("상품 이벤트 발행")
    @Nested
    inner class ProductEventPublishing {
        private lateinit var productService: ProductService
        private lateinit var eventPublisher: FakeEventPublisher

        @BeforeEach
        fun setUp() {
            eventPublisher = FakeEventPublisher()
            productService = ProductService(FakeProductRepository(), eventPublisher)
        }

        @Test
        fun `상품 생성 시 ProductCreatedEvent가 발행된다`() {
            // act
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "https://img.com/p.jpg"),
            )

            // assert
            assertThat(eventPublisher.hasEvent<ProductCreatedEvent>()).isTrue()
            val event = eventPublisher.findEvent<ProductCreatedEvent>()!!
            assertThat(event.productId).isEqualTo(product.id)
            assertThat(event.brandId).isEqualTo(1L)
        }

        @Test
        fun `상품 수정 시 ProductUpdatedEvent가 발행된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "https://img.com/p.jpg"),
            )
            eventPublisher.clear()

            // act
            productService.updateProduct(product.id, ProductCommand.Update(name = "새상품", description = "새설명", price = 20000, stockQuantity = 50, imageUrl = "https://img.com/q.jpg"))

            // assert
            assertThat(eventPublisher.hasEvent<ProductUpdatedEvent>()).isTrue()
        }

        @Test
        fun `상품 삭제 시 ProductDeletedEvent가 발행된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "https://img.com/p.jpg"),
            )
            eventPublisher.clear()

            // act
            productService.deleteProduct(product.id)

            // assert
            assertThat(eventPublisher.hasEvent<ProductDeletedEvent>()).isTrue()
            assertThat(eventPublisher.findEvent<ProductDeletedEvent>()!!.productId).isEqualTo(product.id)
        }

        @Test
        fun `재고 차감 시 StockDeductedEvent가 발행된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "https://img.com/p.jpg"),
            )
            eventPublisher.clear()

            // act
            productService.deductStock(product.id, 5)

            // assert
            assertThat(eventPublisher.hasEvent<StockDeductedEvent>()).isTrue()
            val event = eventPublisher.findEvent<StockDeductedEvent>()!!
            assertThat(event.productId).isEqualTo(product.id)
            assertThat(event.quantity).isEqualTo(5)
        }

        @Test
        fun `재고 복원 시 StockRestoredEvent가 발행된다`() {
            // arrange
            val product = productService.createProduct(
                ProductCommand.Create(brandId = 1L, name = "상품", description = "설명", price = 10000, stockQuantity = 100, imageUrl = "https://img.com/p.jpg"),
            )
            eventPublisher.clear()

            // act
            productService.restoreStock(product.id, 10)

            // assert
            assertThat(eventPublisher.hasEvent<StockRestoredEvent>()).isTrue()
            val event = eventPublisher.findEvent<StockRestoredEvent>()!!
            assertThat(event.quantity).isEqualTo(10)
        }
    }

    @DisplayName("좋아요 이벤트 발행")
    @Nested
    inner class LikeEventPublishing {
        private lateinit var likeService: LikeService
        private lateinit var eventPublisher: FakeEventPublisher

        @BeforeEach
        fun setUp() {
            eventPublisher = FakeEventPublisher()
            likeService = LikeService(FakeProductLikeRepository(), eventPublisher)
        }

        @Test
        fun `좋아요 시 LikeCreatedEvent가 발행된다`() {
            // act
            likeService.like(memberId = 1L, productId = 100L)

            // assert
            assertThat(eventPublisher.hasEvent<LikeCreatedEvent>()).isTrue()
            val event = eventPublisher.findEvent<LikeCreatedEvent>()!!
            assertThat(event.memberId).isEqualTo(1L)
            assertThat(event.productId).isEqualTo(100L)
        }

        @Test
        fun `좋아요 취소 시 LikeCancelledEvent가 발행된다`() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)
            eventPublisher.clear()

            // act
            likeService.unlike(memberId = 1L, productId = 100L)

            // assert
            assertThat(eventPublisher.hasEvent<LikeCancelledEvent>()).isTrue()
            val event = eventPublisher.findEvent<LikeCancelledEvent>()!!
            assertThat(event.memberId).isEqualTo(1L)
            assertThat(event.productId).isEqualTo(100L)
        }
    }

    @DisplayName("주문 이벤트 발행")
    @Nested
    inner class OrderEventPublishing {
        private lateinit var orderService: OrderService
        private lateinit var eventPublisher: FakeEventPublisher

        @BeforeEach
        fun setUp() {
            eventPublisher = FakeEventPublisher()
            orderService = OrderService(FakeOrderRepository(), eventPublisher)
        }

        @Test
        fun `주문 생성 시 OrderCreatedEvent가 발행된다`() {
            // act
            val order = orderService.createOrder(
                memberId = 1L,
                items = listOf(
                    OrderCommand.CreateOrderItem(
                        productId = 1L,
                        quantity = 2,
                        productName = "상품",
                        productPrice = 10000,
                        brandName = "브랜드",
                    ),
                ),
            )

            // assert
            assertThat(eventPublisher.hasEvent<OrderCreatedEvent>()).isTrue()
            val event = eventPublisher.findEvent<OrderCreatedEvent>()!!
            assertThat(event.orderId).isEqualTo(order.id)
            assertThat(event.memberId).isEqualTo(1L)
        }

        @Test
        fun `주문 상태를 PAID로 변경하면 OrderPaidEvent가 발행된다`() {
            // arrange
            val order = orderService.createOrder(
                memberId = 1L,
                items = listOf(
                    OrderCommand.CreateOrderItem(
                        productId = 1L,
                        quantity = 2,
                        productName = "상품",
                        productPrice = 10000,
                        brandName = "브랜드",
                    ),
                ),
            )
            orderService.updateOrderStatus(order.id, OrderStatus.PAYMENT_PENDING)
            eventPublisher.clear()

            // act
            orderService.updateOrderStatus(order.id, OrderStatus.PAID)

            // assert
            assertThat(eventPublisher.hasEvent<OrderPaidEvent>()).isTrue()
            assertThat(eventPublisher.findEvent<OrderPaidEvent>()!!.orderId).isEqualTo(order.id)
        }
    }
}
