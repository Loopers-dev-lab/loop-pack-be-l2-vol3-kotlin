package com.loopers.application.order

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.ProductService
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private var productId: Long = 0L
    private var productName: String = ""
    private var productPrice: Long = 0L
    private var brandName: String = ""
    private val memberId = 1L

    @BeforeEach
    fun setUp() {
        val brand = brandService.createBrand(
            BrandCommand.Create(name = "루퍼스", description = "테스트", imageUrl = "https://example.com/brand.jpg"),
        )
        brandName = brand.name
        val product = productService.createProduct(
            ProductCommand.Create(
                brandId = brand.id,
                name = "감성 티셔츠",
                description = "좋은 상품",
                price = 39000,
                stockQuantity = 100,
                imageUrl = "https://example.com/product.jpg",
            ),
        )
        productId = product.id
        productName = product.name
        productPrice = product.price
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createOrder(): OrderModel {
        val items = listOf(
            OrderCommand.CreateOrderItem(
                productId = productId,
                quantity = 2,
                productName = productName,
                productPrice = productPrice,
                brandName = brandName,
            ),
        )
        return orderService.createOrder(memberId, items)
    }

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenValidInfoProvided() {
            // act
            val order = createOrder()

            // assert
            assertAll(
                { assertThat(order.memberId).isEqualTo(memberId) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(order.items).hasSize(1) },
                { assertThat(order.items[0].productName).isEqualTo("감성 티셔츠") },
                { assertThat(order.items[0].amount).isEqualTo(78000L) },
                { assertThat(order.getTotalAmount()).isEqualTo(78000L) },
            )
        }

        @DisplayName("주문 항목이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsEmpty() {
            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(memberId, emptyList())
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZero() {
            // arrange
            val items = listOf(
                OrderCommand.CreateOrderItem(
                    productId = productId,
                    quantity = 0,
                    productName = productName,
                    productPrice = productPrice,
                    brandName = brandName,
                ),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(memberId, items)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsNegative() {
            // arrange
            val items = listOf(
                OrderCommand.CreateOrderItem(
                    productId = productId,
                    quantity = -1,
                    productName = productName,
                    productPrice = productPrice,
                    brandName = brandName,
                ),
            )

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.createOrder(memberId, items)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문을 조회할 때,")
    @Nested
    inner class GetOrder {
        @DisplayName("본인 주문을 조회하면, 주문을 반환한다.")
        @Test
        fun returnsOrder_whenOwner() {
            // arrange
            val created = createOrder()

            // act
            val result = orderService.getOrder(created.id, memberId)

            // assert
            assertThat(result.id).isEqualTo(created.id)
        }

        @DisplayName("다른 사용자의 주문을 조회하면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenNotOwner() {
            // arrange
            val created = createOrder()

            // act & assert
            val result = assertThrows<CoreException> {
                orderService.getOrder(created.id, 999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @DisplayName("존재하지 않는 주문을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderDoesNotExist() {
            val result = assertThrows<CoreException> {
                orderService.getOrder(999L, memberId)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("어드민 주문을 조회할 때,")
    @Nested
    inner class GetOrderById {
        @DisplayName("존재하는 주문을 조회하면, validateOwner 없이 주문을 반환한다.")
        @Test
        fun returnsOrder_withoutOwnerValidation() {
            // arrange
            val created = createOrder()

            // act
            val result = orderService.getOrderById(created.id)

            // assert
            assertThat(result.id).isEqualTo(created.id)
        }
    }
}
