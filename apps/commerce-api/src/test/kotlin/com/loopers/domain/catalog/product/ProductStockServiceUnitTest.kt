package com.loopers.domain.catalog.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductStockServiceUnitTest {

    private val mockRepository = mockk<ProductStockRepository>()
    private val productStockService = ProductStockService(mockRepository)

    // ─── getByProductId ───

    @Test
    fun `getByProductId() returns stock when it exists`() {
        // Arrange
        val stock = createStock(productId = 1L, quantity = 10)
        every { mockRepository.findByProductId(1L) } returns stock

        // Act
        val result = productStockService.getByProductId(1L)

        // Assert
        assertThat(result.quantity).isEqualTo(10)
    }

    @Test
    fun `getByProductId() throws NOT_FOUND when stock does not exist`() {
        // Arrange
        every { mockRepository.findByProductId(99L) } returns null

        // Act & Assert
        assertThrows<CoreException> {
            productStockService.getByProductId(99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ─── createStock ───

    @Test
    fun `createStock() should save new stock`() {
        // Arrange
        val stock = createStock(productId = 1L, quantity = 100)
        every { mockRepository.save(any()) } returns stock

        // Act
        val result = productStockService.createStock(productId = 1L, quantity = 100)

        // Assert
        assertThat(result.quantity).isEqualTo(100)
        verify { mockRepository.save(any()) }
    }

    // ─── decrementStock ───

    @Test
    fun `decrementStock() should reduce quantity and save`() {
        // Arrange
        val stock = createStock(id = 1L, productId = 1L, quantity = 10)
        every { mockRepository.findByProductIdForUpdate(1L) } returns stock
        every { mockRepository.save(any()) } answers { firstArg() }

        // Act
        val result = productStockService.decrementStock(1L, 3)

        // Assert
        assertThat(result.quantity).isEqualTo(7)
        verify { mockRepository.save(any()) }
    }

    @Test
    fun `decrementStock() throws BAD_REQUEST when qty exceeds stock`() {
        // Arrange
        val stock = createStock(id = 1L, productId = 1L, quantity = 2)
        every { mockRepository.findByProductIdForUpdate(1L) } returns stock

        // Act & Assert
        assertThrows<CoreException> {
            productStockService.decrementStock(1L, 5)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
        verify(exactly = 0) { mockRepository.save(any()) }
    }

    @Test
    fun `decrementStock() throws NOT_FOUND when stock does not exist`() {
        // Arrange
        every { mockRepository.findByProductIdForUpdate(99L) } returns null

        // Act & Assert
        assertThrows<CoreException> {
            productStockService.decrementStock(99L, 1)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ─── updateStock ───

    @Test
    fun `updateStock() should set quantity to new value and save`() {
        // Arrange
        val stock = createStock(id = 1L, productId = 1L, quantity = 10)
        every { mockRepository.findByProductId(1L) } returns stock
        every { mockRepository.save(any()) } answers { firstArg() }

        // Act
        val result = productStockService.updateStock(1L, 50)

        // Assert
        assertThat(result.quantity).isEqualTo(50)
        verify { mockRepository.save(any()) }
    }

    @Test
    fun `updateStock() throws NOT_FOUND when stock does not exist`() {
        // Arrange
        every { mockRepository.findByProductId(99L) } returns null

        // Act & Assert
        assertThrows<CoreException> {
            productStockService.updateStock(99L, 10)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    private fun createStock(
        productId: Long = 1L,
        quantity: Int = 10,
        id: Long = 0L,
    ): ProductStock = ProductStock(productId = productId, quantity = quantity, id = id)
}
