package com.loopers.application.order

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.order.FakeOrderItemRepository
import com.loopers.domain.order.FakeOrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PlaceOrderUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var orderRepository: FakeOrderRepository
    private lateinit var orderItemRepository: FakeOrderItemRepository
    private lateinit var placeOrderUseCase: PlaceOrderUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        orderRepository = FakeOrderRepository()
        orderItemRepository = FakeOrderItemRepository()
        placeOrderUseCase = PlaceOrderUseCase(productRepository, orderRepository, orderItemRepository)
    }

    private fun createProduct(
        price: BigDecimal = BigDecimal("10000"),
        stock: Int = 100,
    ): Product {
        return productRepository.save(
            Product(
                refBrandId = BrandId(1),
                name = "에어맥스 90",
                price = Money(price),
                stock = Stock(stock),
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("정상 주문이 생성되고 재고 차감이 수행된다")
        fun execute_success() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 2)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.id).isNotEqualTo(0L)
            assertThat(orderInfo.status).isEqualTo("CREATED")
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(orderInfo.items).hasSize(1)

            // 재고 차감 확인
            val updatedProduct = productRepository.findById(product.id)!!
            assertThat(updatedProduct.stock.value).isEqualTo(98)
        }

        @Test
        @DisplayName("재고가 부족하면 CoreException이 발생한다")
        fun execute_insufficientStock_throwsException() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 2)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 5)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("삭제된 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun execute_deletedProduct_throwsException() {
            // arrange
            val product = createProduct()
            product.delete()
            productRepository.save(product)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 항목이 비어있으면 BAD_REQUEST 예외가 발생한다")
        fun execute_emptyItems_throwsBadRequest() {
            // arrange
            val command = PlaceOrderCommand(items = emptyList())

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 수량이 0 이하이면 BAD_REQUEST 예외가 발생한다")
        fun execute_zeroQuantity_throwsBadRequest() {
            // arrange
            val product = createProduct(price = BigDecimal("10000"), stock = 100)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 0)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("상품 가격이 소수점 0.5 이상일 때 총 금액이 정상 계산된다")
        fun execute_fractionalPrice_calculatesCorrectTotal() {
            // arrange -- 1000.50원 x 1개 = 1000.50원
            val product = createProduct(price = BigDecimal("1000.50"), stock = 10)
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = product.id.value, quantity = 1)),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("1000.50"))
        }

        @Test
        @DisplayName("여러 상품을 포함한 주문이 정상 생성된다")
        fun execute_multipleItems_success() {
            // arrange
            val product1 = productRepository.save(
                Product(
                    refBrandId = BrandId(1),
                    name = "상품1",
                    price = Money(BigDecimal("10000")),
                    stock = Stock(50),
                ),
            )
            val product2 = productRepository.save(
                Product(
                    refBrandId = BrandId(1),
                    name = "상품2",
                    price = Money(BigDecimal("20000")),
                    stock = Stock(50),
                ),
            )
            val command = PlaceOrderCommand(
                items = listOf(
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product1.id.value, quantity = 2),
                    PlaceOrderCommand.PlaceOrderItemCommand(productId = product2.id.value, quantity = 3),
                ),
            )

            // act
            val orderInfo = placeOrderUseCase.execute(1L, command)

            // assert
            assertThat(orderInfo.items).hasSize(2)
            assertThat(orderInfo.totalPrice).isEqualByComparingTo(BigDecimal("80000"))

            val p1 = productRepository.findById(product1.id)!!
            val p2 = productRepository.findById(product2.id)!!
            assertThat(p1.stock.value).isEqualTo(48)
            assertThat(p2.stock.value).isEqualTo(47)
        }

        @Test
        @DisplayName("존재하지 않는 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        fun execute_nonExistentProduct_throwsBadRequest() {
            // arrange
            val command = PlaceOrderCommand(
                items = listOf(PlaceOrderCommand.PlaceOrderItemCommand(productId = 999L, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                placeOrderUseCase.execute(1L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
