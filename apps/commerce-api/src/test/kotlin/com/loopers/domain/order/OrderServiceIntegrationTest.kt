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
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
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

        @DisplayName("Order가 항목과 함께 DB에 저장된다.")
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
                { assertThat(order.items).hasSize(1) },
            )
        }

        @DisplayName("OrderItem의 스냅샷 정보가 정확히 저장된다.")
        @Test
        fun savesOrderItemSnapshotCorrectly() {
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
            val savedItem = order.items[0]
            assertAll(
                { assertThat(savedItem.productId).isEqualTo(product.id) },
                { assertThat(savedItem.quantity).isEqualTo(2) },
                { assertThat(savedItem.productName).isEqualTo("에어맥스") },
                { assertThat(savedItem.productPrice).isEqualTo(159000L) },
                { assertThat(savedItem.brandName).isEqualTo("나이키") },
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
            assertThat(order.items).hasSize(2)
            // 159000 * 2 + 139000 * 1 = 457000
            assertThat(order.totalAmount).isEqualTo(457000L)
        }
    }

    @DisplayName("주문 목록을 조회할 때,")
    @Nested
    inner class GetOrders {

        @DisplayName("해당 유저의 기간 내 주문만 반환한다.")
        @Test
        fun returnsOrdersWithinPeriod() {
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
            orderService.createOrder(1L, items)

            val startAt = LocalDateTime.now().minusDays(1)
            val endAt = LocalDateTime.now().plusDays(1)

            // act
            val result = orderService.getOrders(1L, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].userId).isEqualTo(1L)
        }

        @DisplayName("다른 유저의 주문은 반환하지 않는다.")
        @Test
        fun doesNotReturnOtherUsersOrders() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(
                OrderItemCommand(
                    productId = product.id,
                    quantity = 1,
                    productName = product.name,
                    productPrice = product.price,
                    brandName = brand.name,
                ),
            )
            orderService.createOrder(1L, items)
            orderService.createOrder(2L, items)

            val startAt = LocalDateTime.now().minusDays(1)
            val endAt = LocalDateTime.now().plusDays(1)

            // act
            val result = orderService.getOrders(1L, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].userId).isEqualTo(1L)
        }

        @DisplayName("기간 외 주문은 반환하지 않는다.")
        @Test
        fun doesNotReturnOrdersOutsidePeriod() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(
                OrderItemCommand(
                    productId = product.id,
                    quantity = 1,
                    productName = product.name,
                    productPrice = product.price,
                    brandName = brand.name,
                ),
            )
            orderService.createOrder(1L, items)

            val startAt = LocalDateTime.now().minusDays(30)
            val endAt = LocalDateTime.now().minusDays(29)

            // act
            val result = orderService.getOrders(1L, startAt, endAt)

            // assert
            assertThat(result).isEmpty()
        }

        @DisplayName("시작일이 종료일보다 크면, BAD_REQUEST 예외를 던진다.")
        @Test
        fun throwsBadRequest_whenStartAtIsAfterEndAt() {
            // arrange
            val startAt = LocalDateTime.now().plusDays(1)
            val endAt = LocalDateTime.now().minusDays(1)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrders(1L, startAt, endAt)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문을 단건 조회할 때,")
    @Nested
    inner class GetOrder {

        @DisplayName("DB에서 주문을 조회한다.")
        @Test
        fun returnsOrderFromDatabase() {
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
            val createdOrder = orderService.createOrder(1L, items)

            // act
            val result = orderService.getOrder(1L, createdOrder.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(createdOrder.id) },
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.totalAmount).isEqualTo(318000L) },
                { assertThat(result.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외를 던진다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(1L, 999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("다른 사용자의 주문이면, FORBIDDEN 예외를 던진다.")
        @Test
        fun throwsForbidden_whenOtherUsersOrder() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val items = listOf(
                OrderItemCommand(
                    productId = product.id,
                    quantity = 1,
                    productName = product.name,
                    productPrice = product.price,
                    brandName = brand.name,
                ),
            )
            val createdOrder = orderService.createOrder(1L, items)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(2L, createdOrder.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
