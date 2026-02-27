package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.Quantity
import com.loopers.domain.common.SortOrder
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
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
class AdminOrderFacadeIntegrationTest @Autowired constructor(
    private val adminOrderFacade: AdminOrderFacade,
    private val orderService: OrderService,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(brand: Brand, name: String = "에어맥스", price: Money = Money.of(159000L)): Product {
        return productRepository.save(
            Product(name = name, description = "러닝화", price = price, likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
        )
    }

    private fun createOrder(userId: Long, brand: Brand, product: Product) {
        val items = listOf(
            OrderItemCommand(
                productId = product.id,
                quantity = Quantity.of(1),
                productName = product.name,
                productPrice = product.price,
                brandName = brand.name,
            ),
        )
        orderService.createOrder(userId, items)
    }

    @DisplayName("어드민 주문 목록 조회할 때,")
    @Nested
    inner class GetOrders {

        private val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

        @DisplayName("주문 목록을 조회하면, 모든 유저의 주문을 반환한다.")
        @Test
        fun returnsAllUsersOrders() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            createOrder(1L, brand, product)
            createOrder(2L, brand, product)

            // act
            val result = adminOrderFacade.getOrders(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(2) },
                { assertThat(result.content.map { it.userId }).containsExactlyInAnyOrder(1L, 2L) },
            )
        }

        @DisplayName("주문 정보에 orderId, userId, totalAmount, status, orderedAt이 포함된다.")
        @Test
        fun returnsOrderInfoFields() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            createOrder(1L, brand, product)

            // act
            val result = adminOrderFacade.getOrders(pageQuery)

            // assert
            val orderInfo = result.content.first()
            assertAll(
                { assertThat(orderInfo.orderId).isNotNull() },
                { assertThat(orderInfo.userId).isEqualTo(1L) },
                { assertThat(orderInfo.totalAmount).isEqualTo(159000L) },
                { assertThat(orderInfo.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(orderInfo.orderedAt).isNotNull() },
            )
        }

        @DisplayName("페이징이 정상적으로 동작한다.")
        @Test
        fun returnsPaginatedOrders() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            repeat(3) { i -> createOrder((i + 1).toLong(), brand, product) }

            val smallPageQuery = PageQuery(0, 2, SortOrder.UNSORTED)

            // act
            val result = adminOrderFacade.getOrders(smallPageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }

        @DisplayName("주문이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoOrders() {
            // act
            val result = adminOrderFacade.getOrders(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0) },
            )
        }
    }
}
