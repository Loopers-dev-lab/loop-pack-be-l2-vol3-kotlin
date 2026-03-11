package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductUnitTest {

    // ─── decrementStock ───

    @Test
    fun `decrementStock() should reduce stock by quantity`() {
        // Arrange
        val product = createProduct(stock = 10)

        // Act
        product.decrementStock(3)

        // Assert
        assertThat(product.stock).isEqualTo(7)
    }

    @Test
    fun `decrementStock() should succeed when quantity equals stock (boundary)`() {
        // Arrange
        val product = createProduct(stock = 5)

        // Act
        product.decrementStock(5)

        // Assert
        assertThat(product.stock).isEqualTo(0)
    }

    @Test
    fun `decrementStock() throws CoreException(BAD_REQUEST) when quantity exceeds stock`() {
        // Arrange
        val product = createProduct(stock = 3)

        // Act & Assert
        assertThrows<CoreException> {
            product.decrementStock(4)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
        assertThat(product.stock).isEqualTo(3) // unchanged
    }

    @Test
    fun `decrementStock() throws CoreException(BAD_REQUEST) when quantity is zero`() {
        // Arrange
        val product = createProduct(stock = 10)

        // Act & Assert
        assertThrows<CoreException> {
            product.decrementStock(0)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `decrementStock() throws CoreException(BAD_REQUEST) when quantity is negative`() {
        // Arrange
        val product = createProduct(stock = 10)

        // Act & Assert
        assertThrows<CoreException> {
            product.decrementStock(-1)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    // ─── validateStock ───

    @Test
    fun `validateStock() should pass when quantity equals stock (exact boundary)`() {
        // Arrange
        val product = createProduct(stock = 5)

        // Act & Assert (no exception)
        product.validateStock(5)
    }

    @Test
    fun `validateStock() throws CoreException(BAD_REQUEST) when quantity exceeds stock`() {
        // Arrange
        val product = createProduct(stock = 3)

        // Act & Assert
        assertThrows<CoreException> {
            product.validateStock(4)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    // ─── incrementLike / decrementLike ───

    @Test
    fun `incrementLike() should increase likeCount by 1`() {
        // Arrange
        val product = createProduct(likeCount = 5)

        // Act
        product.incrementLike()

        // Assert
        assertThat(product.likeCount).isEqualTo(6)
    }

    @Test
    fun `decrementLike() should decrease likeCount by 1`() {
        // Arrange
        val product = createProduct(likeCount = 5)

        // Act
        product.decrementLike()

        // Assert
        assertThat(product.likeCount).isEqualTo(4)
    }

    @Test
    fun `decrementLike() throws CoreException(BAD_REQUEST) when likeCount is 0`() {
        // Arrange
        val product = createProduct(likeCount = 0)

        // Act & Assert
        assertThrows<CoreException> {
            product.decrementLike()
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    // ─── init validation ───

    @Test
    fun `Product init throws CoreException(BAD_REQUEST) when name is blank`() {
        assertThrows<CoreException> {
            createProduct(name = "  ")
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `Product init throws CoreException(BAD_REQUEST) when price is negative`() {
        assertThrows<CoreException> {
            createProduct(price = -1)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `Product init allows price of 0`() {
        // no exception
        val product = createProduct(price = 0)
        assertThat(product.price).isEqualTo(0)
    }

    @Test
    fun `Product init throws CoreException(BAD_REQUEST) when stock is negative`() {
        assertThrows<CoreException> {
            createProduct(stock = -1)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    private fun createProduct(
        id: Long = 0L,
        brandId: Long = 1L,
        name: String = "Test Product",
        description: String = "Test Description",
        price: Int = 10000,
        stock: Int = 100,
        likeCount: Int = 0,
    ): Product = Product(
        id = id,
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stock = stock,
        likeCount = likeCount,
    )
}
