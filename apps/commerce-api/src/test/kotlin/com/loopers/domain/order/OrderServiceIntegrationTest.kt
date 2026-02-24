package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(brand: Brand, name: String = "에어맥스", price: Long = 159000L): Product {
        return productRepository.save(
            Product(name = name, description = "러닝화", price = price, likes = 0, stockQuantity = 100, brandId = brand.id),
        )
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("Order가 DB에 저장된다.")
        @Test
        fun savesOrderToDatabase() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(
                OrderItemCommand(
                    productId = product.id,
                    quantity = 2,
                    productName = product.name,
                    productPrice = product.price,
                    brandName = brand.name,
                ),
            )

            // act
            val order = orderService.createOrder(1L, items)

            // assert
            assertAll(
                { assertThat(order.id).isNotNull() },
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.totalAmount).isEqualTo(318000L) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("OrderItem이 DB에 저장된다.")
        @Test
        fun savesOrderItemsToDatabase() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(
                OrderItemCommand(
                    productId = product.id,
                    quantity = 2,
                    productName = product.name,
                    productPrice = product.price,
                    brandName = brand.name,
                ),
            )

            // act
            val order = orderService.createOrder(1L, items)

            // assert
            val savedItems = orderItemRepository.findByOrderId(order.id)
            assertThat(savedItems).hasSize(1)
            assertAll(
                { assertThat(savedItems[0].productId).isEqualTo(product.id) },
                { assertThat(savedItems[0].quantity).isEqualTo(2) },
                { assertThat(savedItems[0].productName).isEqualTo("에어맥스") },
                { assertThat(savedItems[0].productPrice).isEqualTo(159000L) },
                { assertThat(savedItems[0].brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("여러 상품을 주문하면, 모든 OrderItem이 저장된다.")
        @Test
        fun savesMultipleOrderItems() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(brand, name = "에어맥스", price = 159000L)
            val product2 = createProduct(brand, name = "에어포스", price = 139000L)
            val items = listOf(
                OrderItemCommand(
                    productId = product1.id,
                    quantity = 2,
                    productName = product1.name,
                    productPrice = product1.price,
                    brandName = brand.name,
                ),
                OrderItemCommand(
                    productId = product2.id,
                    quantity = 1,
                    productName = product2.name,
                    productPrice = product2.price,
                    brandName = brand.name,
                ),
            )

            // act
            val order = orderService.createOrder(1L, items)

            // assert
            val savedItems = orderItemRepository.findByOrderId(order.id)
            assertThat(savedItems).hasSize(2)
            // 159000 * 2 + 139000 * 1 = 457000
            assertThat(order.totalAmount).isEqualTo(457000L)
        }
    }
}
