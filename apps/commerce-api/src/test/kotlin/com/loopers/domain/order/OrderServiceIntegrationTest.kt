package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val orderJpaRepository: OrderJpaRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문 생성")
    @Nested
    inner class CreateOrder {

        @DisplayName("OrderService.createOrder()는 Order 저장 후 addItem()을 호출하므로 orderId가 자동으로 올바르게 설정된다")
        @Test
        fun createsOrderWithItems_setsOrderIdCorrectly_forAllOrderItems() {
            // arrange
            val userId = 1L
            val brand = brandRepository.save(Brand.create("Test Brand", "Test Description"))
            val product1 = productRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 1",
                    price = BigDecimal("10000"),
                    status = ProductStatus.ACTIVE,
                ),
            )
            val product2 = productRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 2",
                    price = BigDecimal("20000"),
                    status = ProductStatus.ACTIVE,
                ),
            )

            val items = listOf(
                CreateOrderItemCommand(
                    productId = product1.id,
                    productName = "Product 1",
                    quantity = 2,
                    price = BigDecimal("10000"),
                ),
                CreateOrderItemCommand(
                    productId = product2.id,
                    productName = "Product 2",
                    quantity = 1,
                    price = BigDecimal("20000"),
                ),
            )

            // act
            // 1. Order.create()
            // 2. orderRepository.save() ← 이 시점에 Order.id 할당
            // 3. addItem() ← savedOrder.id는 이미 올바른 값이므로 orderId가 올바르게 복사됨
            val order = orderService.createOrder(userId, items, couponId = null)

            // assert - Order와 OrderItems의 orderId
            assertThat(order.id).isNotEqualTo(0L)
            assertThat(order.orderItems).hasSize(2)

            // assert - 모든 OrderItem의 orderId가 Order.id와 같은지 확인
            // (setOrderItemIds()를 호출하지 않아도 OrderService가 저장 후 addItem()을 호출하므로 자동 설정됨)
            order.orderItems.forEach { item ->
                assertThat(item.orderId).isEqualTo(order.id)
                assertThat(item.productName).isIn("Product 1", "Product 2")
            }

            // assert - 첫 번째 항목 검증
            assertThat(order.orderItems[0].orderId).isEqualTo(order.id)
            assertThat(order.orderItems[0].productId).isEqualTo(product1.id)
            assertThat(order.orderItems[0].quantity).isEqualTo(2)

            // assert - 두 번째 항목 검증
            assertThat(order.orderItems[1].orderId).isEqualTo(order.id)
            assertThat(order.orderItems[1].productId).isEqualTo(product2.id)
            assertThat(order.orderItems[1].quantity).isEqualTo(1)
        }

    }
}
