package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductServiceUnitTest {

    private val mockRepository = mockk<ProductRepository>()
    private val productService = ProductService(mockRepository)

    // ─── createProduct ───

    @Test
    fun `createProduct() should create product with valid data`() {
        // Arrange
        every { mockRepository.save(any()) } returns createProduct(name = "Shoes")

        // Act
        val product = productService.createProduct(
            brandId = 1L,
            name = "Shoes",
            description = "Running shoes",
            price = 50000,
            stock = 100,
        )

        // Assert
        assertThat(product.name).isEqualTo("Shoes")
        verify { mockRepository.save(any()) }
    }

    // ─── getById ───

    @Test
    fun `getById() throws CoreException(NOT_FOUND) when product does not exist`() {
        // Arrange
        every { mockRepository.findById(99L) } returns null

        // Act & Assert
        assertThrows<CoreException> {
            productService.getById(99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Test
    fun `getById() returns product when it exists`() {
        // Arrange
        val product = createProduct(id = 1L, name = "Shoes")
        every { mockRepository.findById(1L) } returns product

        // Act
        val result = productService.getById(1L)

        // Assert
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.name).isEqualTo("Shoes")
    }

    // ─── incrementLikeCount ───

    @Test
    fun `incrementLikeCount() should call product incrementLike and save`() {
        // Arrange
        val product = createProduct(id = 1L, likeCount = 5)
        every { mockRepository.findById(1L) } returns product
        every { mockRepository.save(any()) } answers { firstArg() }

        // Act
        val result = productService.incrementLikeCount(1L)

        // Assert
        assertThat(result.likeCount).isEqualTo(6)
        verify { mockRepository.save(any()) }
    }

    // ─── decrementLikeCount ───

    @Test
    fun `decrementLikeCount() should call product decrementLike and save`() {
        // Arrange
        val product = createProduct(id = 1L, likeCount = 5)
        every { mockRepository.findById(1L) } returns product
        every { mockRepository.save(any()) } answers { firstArg() }

        // Act
        val result = productService.decrementLikeCount(1L)

        // Assert
        assertThat(result.likeCount).isEqualTo(4)
        verify { mockRepository.save(any()) }
    }

    // ─── decrementStock ───

    @Test
    fun `decrementStock() should reduce stock and save`() {
        // Arrange
        val product = createProduct(id = 1L, stock = 10)
        every { mockRepository.findById(1L) } returns product
        every { mockRepository.save(any()) } answers { firstArg() }

        // Act
        val result = productService.decrementStock(1L, 3)

        // Assert
        assertThat(result.stock).isEqualTo(7)
        verify { mockRepository.save(any()) }
    }

    @Test
    fun `decrementStock() throws BAD_REQUEST when quantity exceeds stock`() {
        // Arrange
        val product = createProduct(id = 1L, stock = 2)
        every { mockRepository.findById(1L) } returns product

        // Act & Assert
        assertThrows<CoreException> {
            productService.decrementStock(1L, 5)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
        verify(exactly = 0) { mockRepository.save(any()) }
    }

    @Test
    fun `decrementStock() throws NOT_FOUND when product does not exist`() {
        // Arrange
        every { mockRepository.findById(99L) } returns null

        // Act & Assert
        assertThrows<CoreException> {
            productService.decrementStock(99L, 1)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ─── deleteAllByBrandId ───

    @Test
    fun `deleteAllByBrandId() should call repository deleteAllByBrandId`() {
        // Arrange
        every { mockRepository.deleteAllByBrandId(1L) } returns Unit

        // Act
        productService.deleteAllByBrandId(1L)

        // Assert
        verify { mockRepository.deleteAllByBrandId(1L) }
    }

    // ─── findAll ───

    @Test
    fun `findAll() should return products matching condition`() {
        // Arrange
        val products = listOf(createProduct(id = 1L), createProduct(id = 2L))
        every { mockRepository.findAll(any()) } returns products

        // Act
        val result = productService.findAll(ProductSearchCondition())

        // Assert
        assertThat(result).hasSize(2)
    }

    @Test
    fun `findAll() should return empty list when no products match`() {
        // Arrange
        every { mockRepository.findAll(any()) } returns emptyList()

        // Act
        val result = productService.findAll(ProductSearchCondition())

        // Assert
        assertThat(result).isEmpty()
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
