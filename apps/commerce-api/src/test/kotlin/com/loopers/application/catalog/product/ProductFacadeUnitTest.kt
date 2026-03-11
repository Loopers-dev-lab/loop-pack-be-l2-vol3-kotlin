package com.loopers.application.catalog.product

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.Product
import com.loopers.domain.catalog.product.ProductSearchCondition
import com.loopers.domain.catalog.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductFacadeUnitTest {

    private val mockProductService = mockk<ProductService>()
    private val mockBrandService = mockk<BrandService>()

    private val productFacade = ProductFacade(mockProductService, mockBrandService)

    // ─── createProduct ───

    @Test
    fun `createProduct() should validate brand exists then create product`() {
        // Arrange
        val brand = createBrand(id = 1L, name = "Nike")
        val product = createProduct(id = 1L, brandId = 1L, name = "Shoes")
        every { mockBrandService.getById(1L) } returns brand
        every { mockProductService.createProduct(any(), any(), any(), any(), any()) } returns product

        // Act
        val result = productFacade.createProduct(
            CreateProductCommand(brandId = 1L, name = "Shoes", description = "desc", price = 50000, stock = 100)
        )

        // Assert
        assertThat(result.name).isEqualTo("Shoes")
        assertThat(result.brand.name).isEqualTo("Nike")
        verify { mockBrandService.getById(1L) }
        verify { mockProductService.createProduct(1L, "Shoes", "desc", 50000, 100) }
    }

    @Test
    fun `createProduct() throws NOT_FOUND when brand does not exist`() {
        // Arrange
        every { mockBrandService.getById(99L) } throws CoreException(ErrorType.NOT_FOUND, "브랜드가 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            productFacade.createProduct(
                CreateProductCommand(brandId = 99L, name = "Shoes", description = "desc", price = 50000, stock = 100)
            )
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        verify(exactly = 0) { mockProductService.createProduct(any(), any(), any(), any(), any()) }
    }

    // ─── getProductDetail ───

    @Test
    fun `getProductDetail() should return ProductDetailResult with brand info`() {
        // Arrange
        val product = createProduct(id = 1L, brandId = 1L)
        val brand = createBrand(id = 1L, name = "Nike")
        every { mockProductService.getActiveById(1L) } returns product
        every { mockBrandService.getById(1L) } returns brand

        // Act
        val result = productFacade.getProductDetail(1L)

        // Assert
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.brand.name).isEqualTo("Nike")
    }

    @Test
    fun `getProductDetail() throws NOT_FOUND when product does not exist`() {
        // Arrange
        every { mockProductService.getActiveById(99L) } throws CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            productFacade.getProductDetail(99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ─── findProducts ───

    @Test
    fun `findProducts() should return list with brand info`() {
        // Arrange
        val products = listOf(createProduct(id = 1L, brandId = 1L), createProduct(id = 2L, brandId = 1L))
        val brand = createBrand(id = 1L, name = "Nike")
        every { mockProductService.findAll(any()) } returns products
        every { mockBrandService.getById(1L) } returns brand

        // Act
        val result = productFacade.findProducts(ProductSearchCondition())

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].brandName).isEqualTo("Nike")
    }

    // ─── updateProduct ───

    @Test
    fun `updateProduct() should return updated ProductDetailResult`() {
        // Arrange
        val updated = createProduct(id = 1L, brandId = 1L, name = "Updated Shoes", price = 60000)
        val brand = createBrand(id = 1L, name = "Nike")
        every { mockProductService.update(1L, "Updated Shoes", "new desc", 60000, 50) } returns updated
        every { mockBrandService.getById(1L) } returns brand

        // Act
        val result = productFacade.updateProduct(
            1L, UpdateProductCommand(name = "Updated Shoes", description = "new desc", price = 60000, stock = 50)
        )

        // Assert
        assertThat(result.name).isEqualTo("Updated Shoes")
        assertThat(result.price).isEqualTo(60000)
        assertThat(result.brand.name).isEqualTo("Nike")
    }

    @Test
    fun `updateProduct() throws NOT_FOUND when product does not exist`() {
        // Arrange
        every { mockProductService.update(99L, any(), any(), any(), any()) } throws CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            productFacade.updateProduct(99L, UpdateProductCommand(name = "X", description = "desc", price = 1000, stock = 1))
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    private fun createProduct(
        id: Long = 0L,
        brandId: Long = 1L,
        name: String = "Test Product",
        price: Int = 10000,
    ): Product = Product(id = id, brandId = brandId, name = name, description = "desc", price = price, stock = 100)

    private fun createBrand(id: Long = 0L, name: String = "TestBrand"): Brand =
        Brand(id = id, name = name, description = "desc")
}
