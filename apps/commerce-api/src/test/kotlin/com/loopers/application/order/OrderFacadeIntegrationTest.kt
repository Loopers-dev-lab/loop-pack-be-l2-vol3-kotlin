package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

/**
 * OrderFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - 비관적 락 + 재고 차감 + 주문 생성 원자성 검증
 */
@SpringBootTest
class OrderFacadeIntegrationTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val orderJpaRepository: OrderJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandJpaRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(
        brand: Brand = createBrand(),
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
    ): Product {
        return productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = name,
                price = price,
                stock = stock,
                description = null,
                imageUrl = null,
            ),
        )
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {

        @DisplayName("모든 상품의 재고가 충분하면, 주문이 DB에 저장되고 재고가 차감된다.")
        @Test
        fun savesOrderToDatabase_whenAllStockSufficient() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 100)
            val commands = listOf(OrderItemCommand(productId = product.id, quantity = 2))

            // act
            val result = orderFacade.createOrder(userId = 1L, commands = commands)

            // assert
            val savedOrder = orderJpaRepository.findByIdWithItems(result.order.id)!!
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(savedOrder.userId).isEqualTo(1L) },
                { assertThat(savedOrder.totalAmount).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(savedOrder.orderItems).hasSize(1) },
                { assertThat(updatedProduct.stock).isEqualTo(98) },
                { assertThat(result.excludedItems).isEmpty() },
            )
        }

        @DisplayName("일부 상품의 재고가 부족하면, 부분 주문이 생성되고 excludedItems가 반환된다.")
        @Test
        fun createsPartialOrder_whenSomeStockInsufficient() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(brand = brand, name = "에어맥스 90", stock = 100)
            val product2 = createProduct(brand = brand, name = "에어포스 1", stock = 0)
            val commands = listOf(
                OrderItemCommand(productId = product1.id, quantity = 2),
                OrderItemCommand(productId = product2.id, quantity = 1),
            )

            // act
            val result = orderFacade.createOrder(userId = 1L, commands = commands)

            // assert
            assertAll(
                { assertThat(result.order.orderItems).hasSize(1) },
                { assertThat(result.excludedItems).hasSize(1) },
                { assertThat(result.excludedItems[0].productId).isEqualTo(product2.id) },
            )
        }

        @DisplayName("모든 상품의 재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenAllStockInsufficient() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand = brand, stock = 0)
            val commands = listOf(OrderItemCommand(productId = product.id, quantity = 1))

            // act & assert
            val exception = assertThrows<CoreException> {
                orderFacade.createOrder(userId = 1L, commands = commands)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 생성 시, OrderItem에 상품명/단가/브랜드명 스냅샷이 저장된다.")
        @Test
        fun savesSnapshot_whenOrderCreated() {
            // arrange
            val brand = createBrand(name = "나이키")
            val product = createProduct(brand = brand, name = "에어맥스 90", price = BigDecimal("129000"), stock = 100)
            val commands = listOf(OrderItemCommand(productId = product.id, quantity = 1))

            // act
            val result = orderFacade.createOrder(userId = 1L, commands = commands)

            // assert
            val savedOrder = orderJpaRepository.findByIdWithItems(result.order.id)!!
            val item = savedOrder.orderItems[0]
            assertAll(
                { assertThat(item.productName).isEqualTo("에어맥스 90") },
                { assertThat(item.unitPrice).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(item.brandName).isEqualTo("나이키") },
            )
        }
    }
}
