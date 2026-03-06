package com.loopers.domain.order

import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import com.loopers.support.error.OrderValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class OrderValidatorTest {

    private val validator = OrderValidator()

    private fun createProduct(
        id: Long = 1L,
        brandId: Long = 1L,
        name: String = "테스트 상품",
        price: Money = Money(10000),
        deleted: Boolean = false,
    ): Product {
        val product = Product.create(
            brandId = brandId,
            name = name,
            description = "상품 설명",
            price = price,
            imageUrl = "https://example.com/image.jpg",
        )
        if (deleted) product.delete()
        val idField = product.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(product, id)
        return product
    }

    private fun createProductStock(productId: Long, stock: Stock): ProductStock {
        val productStock = ProductStock.create(productId = productId, stock = stock)
        val idField = productStock.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(productStock, productId)
        return productStock
    }

    private fun orderLine(productId: Long = 1L, quantity: Int = 1): OrderLine {
        return OrderLine(productId = productId, quantity = Quantity(quantity))
    }

    @DisplayName("주문 검증")
    @Nested
    inner class Validate {

        @DisplayName("모든 상품이 정상이면 검증을 통과한다")
        @Test
        fun successWhenAllValid() {
            val lines = listOf(orderLine(1L, 2), orderLine(2L, 3))
            val products = mapOf(
                1L to createProduct(id = 1L),
                2L to createProduct(id = 2L),
            )
            val stocks = mapOf(
                1L to createProductStock(1L, Stock(10)),
                2L to createProductStock(2L, Stock(10)),
            )

            assertDoesNotThrow { validator.validate(lines, products, stocks) }
        }

        @DisplayName("빈 주문 항목이면 EMPTY_ORDER_ITEMS 예외가 발생한다")
        @Test
        fun failWhenEmpty() {
            val exception = assertThrows<CoreException> {
                validator.validate(emptyList(), emptyMap(), emptyMap())
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EMPTY_ORDER_ITEMS)
        }

        @DisplayName("중복 상품이면 DUPLICATE_ORDER_ITEM 예외가 발생한다")
        @Test
        fun failWhenDuplicate() {
            val lines = listOf(orderLine(1L, 1), orderLine(1L, 2))
            val products = mapOf(1L to createProduct(id = 1L))

            val exception = assertThrows<CoreException> {
                validator.validate(lines, products, emptyMap())
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.DUPLICATE_ORDER_ITEM)
        }

        @DisplayName("20종 초과면 EXCEED_MAX_ORDER_TYPES 예외가 발생한다")
        @Test
        fun failWhenExceedMaxTypes() {
            val lines = (1L..21L).map { orderLine(it, 1) }
            val products = (1L..21L).associateWith { createProduct(id = it) }

            val exception = assertThrows<CoreException> {
                validator.validate(lines, products, emptyMap())
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EXCEED_MAX_ORDER_TYPES)
        }

        @DisplayName("99개 초과면 EXCEED_MAX_ORDER_QUANTITY 예외가 발생한다")
        @Test
        fun failWhenExceedMaxQuantity() {
            val lines = listOf(orderLine(1L, 100))
            val products = mapOf(1L to createProduct(id = 1L))

            val exception = assertThrows<CoreException> {
                validator.validate(lines, products, emptyMap())
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EXCEED_MAX_ORDER_QUANTITY)
        }

        @DisplayName("미존재 상품이면 오류를 수집한다")
        @Test
        fun collectErrorWhenProductNotFound() {
            val lines = listOf(orderLine(999L, 1))
            val products = emptyMap<Long, Product>()

            val exception = assertThrows<OrderValidationException> {
                validator.validate(lines, products, emptyMap())
            }

            assertThat(exception.errors).hasSize(1)
            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_NOT_FOUND")
        }

        @DisplayName("삭제된 상품이면 오류를 수집한다")
        @Test
        fun collectErrorWhenProductDeleted() {
            val lines = listOf(orderLine(1L, 1))
            val products = mapOf(1L to createProduct(id = 1L, deleted = true))

            val exception = assertThrows<OrderValidationException> {
                validator.validate(lines, products, emptyMap())
            }

            assertThat(exception.errors).hasSize(1)
            assertThat(exception.errors[0].reason).isEqualTo("PRODUCT_DELETED")
        }

        @DisplayName("재고 부족이면 오류를 수집한다")
        @Test
        fun collectErrorWhenInsufficientStock() {
            val lines = listOf(orderLine(1L, 10))
            val products = mapOf(1L to createProduct(id = 1L))
            val stocks = mapOf(1L to createProductStock(1L, Stock(5)))

            val exception = assertThrows<OrderValidationException> {
                validator.validate(lines, products, stocks)
            }

            assertThat(exception.errors).hasSize(1)
            assertThat(exception.errors[0].reason).isEqualTo("INSUFFICIENT_STOCK")
        }

        @DisplayName("복합 오류(삭제 + 재고 부족)이면 모두 수집한다")
        @Test
        fun collectMultipleErrors() {
            val lines = listOf(orderLine(1L, 1), orderLine(2L, 10))
            val products = mapOf(
                1L to createProduct(id = 1L, deleted = true),
                2L to createProduct(id = 2L),
            )
            val stocks = mapOf(2L to createProductStock(2L, Stock(5)))

            val exception = assertThrows<OrderValidationException> {
                validator.validate(lines, products, stocks)
            }

            assertThat(exception.errors).hasSize(2)
        }
    }
}
