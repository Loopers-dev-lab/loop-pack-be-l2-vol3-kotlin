package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrandFacadeUnitTest {

    private val mockBrandService = mockk<BrandService>()
    private val mockProductService = mockk<ProductService>()

    private val brandFacade = BrandFacade(mockBrandService, mockProductService)

    // ─── deleteBrand ───

    @Test
    fun `deleteBrand() should delete products first then brand`() {
        // Arrange
        val brand = createBrand(id = 1L)
        every { mockBrandService.getById(1L) } returns brand
        every { mockProductService.deleteAllByBrandId(1L) } returns Unit
        every { mockBrandService.delete(1L) } returns Unit

        // Act
        brandFacade.deleteBrand(1L)

        // Assert
        verifyOrder {
            mockBrandService.getById(1L)
            mockProductService.deleteAllByBrandId(1L)
            mockBrandService.delete(1L)
        }
    }

    @Test
    fun `deleteBrand() throws NOT_FOUND when brand does not exist`() {
        // Arrange
        every { mockBrandService.getById(99L) } throws CoreException(ErrorType.NOT_FOUND, "브랜드가 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            brandFacade.deleteBrand(99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        verify(exactly = 0) { mockProductService.deleteAllByBrandId(any()) }
        verify(exactly = 0) { mockBrandService.delete(any()) }
    }

    private fun createBrand(
        id: Long = 0L,
        name: String = "TestBrand",
        description: String = "Test Description",
    ): Brand = Brand(id = id, name = name, description = description)
}
