package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_상품을_생성할_수_있다`() {
            // act
            val product = createProduct()

            // assert
            assertAll(
                { assertThat(product.brandId).isEqualTo(1L) },
                { assertThat(product.name.value).isEqualTo("상품명") },
                { assertThat(product.price.value).isEqualTo(10000L) },
                { assertThat(product.description.value).isEqualTo("설명") },
                { assertThat(product.stock.value).isEqualTo(100) },
                { assertThat(product.status).isEqualTo(ProductStatus.SELLING) },
            )
        }
    }

    @Nested
    inner class ChangeInfo {
        @Test
        fun `상품_정보를_변경할_수_있다`() {
            // arrange
            val product = createProduct()

            // act
            product.changeInfo(
                name = ProductName("새상품명"),
                price = ProductPrice(20000),
                description = ProductDescription("새설명"),
            )

            // assert
            assertAll(
                { assertThat(product.name.value).isEqualTo("새상품명") },
                { assertThat(product.price.value).isEqualTo(20000L) },
                { assertThat(product.description.value).isEqualTo("새설명") },
            )
        }
    }

    @Nested
    inner class StopSelling {
        @Test
        fun `판매중인_상품을_판매중지할_수_있다`() {
            // arrange
            val product = createProduct()

            // act
            product.stopSelling()

            // assert
            assertThat(product.status).isEqualTo(ProductStatus.STOP_SELLING)
        }

        @Test
        fun `이미_판매중지된_상품을_판매중지하면_예외가_발생한다`() {
            // arrange
            val product = createProduct()
            product.stopSelling()

            // act
            val result = assertThrows<CoreException> { product.stopSelling() }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.PRODUCT_ALREADY_STOP_SELLING)
        }
    }

    @Nested
    inner class DeductStock {
        @Test
        fun `재고를_차감할_수_있다`() {
            // arrange
            val product = createProduct(stock = 10)

            // act
            product.deductStock(3)

            // assert
            assertThat(product.stock.value).isEqualTo(7)
        }

        @Test
        fun `재고가_부족하면_예외가_발생한다`() {
            // arrange
            val product = createProduct(stock = 3)

            // act
            val result = assertThrows<CoreException> { product.deductStock(5) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INSUFFICIENT_STOCK)
        }
    }

    @Nested
    inner class RestoreStock {
        @Test
        fun `재고를_복원할_수_있다`() {
            // arrange
            val product = createProduct(stock = 5)

            // act
            product.restoreStock(3)

            // assert
            assertThat(product.stock.value).isEqualTo(8)
        }
    }

    private fun createProduct(
        brandId: Long = 1L,
        name: String = "상품명",
        price: Long = 10000L,
        description: String = "설명",
        stock: Int = 100,
    ): Product {
        return Product(
            brandId = brandId,
            name = ProductName(name),
            price = ProductPrice(price),
            description = ProductDescription(description),
            stock = Stock(stock),
        )
    }
}
