package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.ProductStock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("OrderDomainService")
class OrderDomainServiceTest {

    private fun createSnapshot(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        brandId: Long = 1L,
        brandName: String = "테스트 브랜드",
        regularPrice: Money = Money(BigDecimal("10000")),
        sellingPrice: Money = Money(BigDecimal("8000")),
    ): OrderSnapshot = OrderSnapshot(
        productId = productId,
        productName = productName,
        brandId = brandId,
        brandName = brandName,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        thumbnailUrl = null,
    )

    private fun createStock(
        id: Long = 1L,
        productId: Long = 1L,
        quantity: Int = 10,
    ): ProductStock = ProductStock.retrieve(id = id, productId = productId, quantity = Quantity(quantity))

    @Nested
    @DisplayName("재고가 충분하면 주문이 성공한다")
    inner class WhenStockSufficient {

        @Test
        @DisplayName("정상 흐름: 재고 검증 통과 → 차감 → Order 생성")
        fun createOrder_success() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(id = 1L, productId = 1L, quantity = 10),
                    snapshot = createSnapshot(productId = 1L, productName = "상품A"),
                    quantity = Quantity(3),
                ),
            )

            // act
            val result = OrderDomainService.createOrder(
                userId = 1L,
                idempotencyKey = IdempotencyKey("test-key-1"),
                orderItemRequests = requests,
            )

            // assert
            assertAll(
                { assertThat(result.order.userId).isEqualTo(1L) },
                { assertThat(result.order.status).isEqualTo(Order.Status.CREATED) },
                { assertThat(result.order.items).hasSize(1) },
                { assertThat(result.order.items[0].quantity.value).isEqualTo(3) },
            )
        }

        @Test
        @DisplayName("재고와 주문 수량이 동일하면 성공한다 (경계값)")
        fun createOrder_exactStock() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(quantity = 5),
                    snapshot = createSnapshot(),
                    quantity = Quantity(5),
                ),
            )

            // act
            val result = OrderDomainService.createOrder(
                userId = 1L,
                idempotencyKey = IdempotencyKey("test-key-2"),
                orderItemRequests = requests,
            )

            // assert
            assertThat(result.order.items[0].quantity.value).isEqualTo(5)
        }

        @Test
        @DisplayName("여러 상품 주문 시 모든 재고 차감 결과를 반환한다")
        fun createOrder_multipleItems_decreasedStocks() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(id = 1L, productId = 1L, quantity = 10),
                    snapshot = createSnapshot(productId = 1L),
                    quantity = Quantity(3),
                ),
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(id = 2L, productId = 2L, quantity = 20),
                    snapshot = createSnapshot(productId = 2L, productName = "상품B"),
                    quantity = Quantity(7),
                ),
            )

            // act
            val result = OrderDomainService.createOrder(
                userId = 1L,
                idempotencyKey = IdempotencyKey("test-key-3"),
                orderItemRequests = requests,
            )

            // assert
            assertAll(
                { assertThat(result.order.items).hasSize(2) },
                { assertThat(result.decreasedStocks).hasSize(2) },
                { assertThat(result.decreasedStocks[0].quantity.value).isEqualTo(7) },
                { assertThat(result.decreasedStocks[1].quantity.value).isEqualTo(13) },
            )
        }
    }

    @Nested
    @DisplayName("재고가 부족하면 주문이 실패한다")
    inner class WhenStockInsufficient {

        @Test
        @DisplayName("재고 부족 시 PRODUCT_STOCK_INSUFFICIENT 예외 + 부족 정보 반환")
        fun createOrder_insufficientStock() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(productId = 1L, quantity = 2),
                    snapshot = createSnapshot(productId = 1L, productName = "상품A"),
                    quantity = Quantity(5),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-4"),
                    orderItemRequests = requests,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_STOCK_INSUFFICIENT)
            @Suppress("UNCHECKED_CAST")
            val insufficientList = exception.data as List<OrderDomainService.InsufficientStockInfo>
            assertAll(
                { assertThat(insufficientList).hasSize(1) },
                { assertThat(insufficientList[0].productId).isEqualTo(1L) },
                { assertThat(insufficientList[0].productName).isEqualTo("상품A") },
                { assertThat(insufficientList[0].requestedQuantity).isEqualTo(5) },
                { assertThat(insufficientList[0].availableQuantity).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("여러 상품의 재고가 부족하면 모든 부족 정보를 수집한다")
        fun createOrder_multipleInsufficientStocks() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(productId = 1L, quantity = 2),
                    snapshot = createSnapshot(productId = 1L, productName = "상품A"),
                    quantity = Quantity(5),
                ),
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(productId = 2L, quantity = 0),
                    snapshot = createSnapshot(productId = 2L, productName = "상품B"),
                    quantity = Quantity(3),
                ),
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(productId = 3L, quantity = 100),
                    snapshot = createSnapshot(productId = 3L, productName = "상품C"),
                    quantity = Quantity(1),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-5"),
                    orderItemRequests = requests,
                )
            }

            // assert — 상품A, 상품B만 부족 (상품C는 충분)
            @Suppress("UNCHECKED_CAST")
            val insufficientList = exception.data as List<OrderDomainService.InsufficientStockInfo>
            assertAll(
                { assertThat(insufficientList).hasSize(2) },
                { assertThat(insufficientList[0].productId).isEqualTo(1L) },
                { assertThat(insufficientList[1].productId).isEqualTo(2L) },
            )
        }

        @Test
        @DisplayName("재고 0, 주문 1 → 실패 (경계값)")
        fun createOrder_zeroStock() {
            // arrange
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(quantity = 0),
                    snapshot = createSnapshot(),
                    quantity = Quantity(1),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-6"),
                    orderItemRequests = requests,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_STOCK_INSUFFICIENT)
        }
    }

    @Nested
    @DisplayName("Precondition 위반 시 실패한다")
    inner class WhenPreconditionViolated {

        @Test
        @DisplayName("ProductStock.productId와 OrderSnapshot.productId가 다르면 실패한다")
        fun createOrder_productIdMismatch() {
            // arrange — stock은 productId=1, snapshot은 productId=2
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(productId = 1L, quantity = 10),
                    snapshot = createSnapshot(productId = 2L),
                    quantity = Quantity(3),
                ),
            )

            // act & assert
            assertThrows<IllegalArgumentException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-mismatch"),
                    orderItemRequests = requests,
                )
            }
        }

        @Test
        @DisplayName("동일 상품이 중복으로 포함되면 실패한다")
        fun createOrder_duplicateProductId() {
            // arrange — 같은 productId=1이 두 번
            val requests = listOf(
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(id = 1L, productId = 1L, quantity = 10),
                    snapshot = createSnapshot(productId = 1L, productName = "상품A"),
                    quantity = Quantity(3),
                ),
                OrderDomainService.OrderItemRequest(
                    productStock = createStock(id = 1L, productId = 1L, quantity = 10),
                    snapshot = createSnapshot(productId = 1L, productName = "상품A"),
                    quantity = Quantity(2),
                ),
            )

            // act & assert
            assertThrows<IllegalArgumentException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-duplicate"),
                    orderItemRequests = requests,
                )
            }
        }

        @Test
        @DisplayName("빈 주문 항목 리스트 → Order.create()에서 실패한다")
        fun createOrder_emptyRequests() {
            // act & assert
            assertThrows<CoreException> {
                OrderDomainService.createOrder(
                    userId = 1L,
                    idempotencyKey = IdempotencyKey("test-key-empty"),
                    orderItemRequests = emptyList(),
                )
            }
        }
    }
}
