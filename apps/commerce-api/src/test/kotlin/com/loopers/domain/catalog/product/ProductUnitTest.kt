package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductUnitTest {

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
        val product = createProduct(price = 0)
        assertThat(product.price).isEqualTo(0)
    }

    // ─── update ───

    @Test
    fun `update() should change name, description and price`() {
        // Arrange
        val product = createProduct(name = "Old", price = 1000)

        // Act
        product.update(name = "New", description = "new desc", price = 2000)

        // Assert
        assertThat(product.name).isEqualTo("New")
        assertThat(product.description).isEqualTo("new desc")
        assertThat(product.price).isEqualTo(2000)
    }

    @Test
    fun `update() throws BAD_REQUEST when name is blank`() {
        val product = createProduct()
        assertThrows<CoreException> {
            product.update(name = "  ", description = "desc", price = 1000)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Test
    fun `update() throws BAD_REQUEST when price is negative`() {
        val product = createProduct()
        assertThrows<CoreException> {
            product.update(name = "Name", description = "desc", price = -1)
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
        likeCount: Int = 0,
    ): Product = Product(
        id = id,
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        likeCount = likeCount,
    )
}
