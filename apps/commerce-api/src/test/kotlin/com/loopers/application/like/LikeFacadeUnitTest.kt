package com.loopers.application.like

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.Product
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LikeFacadeUnitTest {

    private val mockLikeService = mockk<LikeService>()
    private val mockProductService = mockk<ProductService>()
    private val mockBrandService = mockk<BrandService>()

    private val likeFacade = LikeFacade(mockLikeService, mockProductService, mockBrandService)

    // ─── addLike ───

    @Test
    fun `addLike() should call productService then likeService then incrementLikeCount in order`() {
        // Arrange
        val product = createProduct(id = 10L)
        every { mockProductService.getById(10L) } returns product
        every { mockLikeService.addLike(1L, 10L) } returns createLike(userId = 1L, productId = 10L)
        every { mockProductService.incrementLikeCount(10L) } just Runs

        // Act
        likeFacade.addLike(userId = 1L, productId = 10L)

        // Assert
        verifyOrder {
            mockProductService.getById(10L)
            mockLikeService.addLike(1L, 10L)
            mockProductService.incrementLikeCount(10L)
        }
    }

    @Test
    fun `addLike() throws NOT_FOUND when product does not exist`() {
        // Arrange
        every { mockProductService.getById(99L) } throws CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            likeFacade.addLike(userId = 1L, productId = 99L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        verify(exactly = 0) { mockLikeService.addLike(any(), any()) }
        verify(exactly = 0) { mockProductService.incrementLikeCount(any()) }
    }

    // ─── removeLike ───

    @Test
    fun `removeLike() should call likeService then decrementLikeCount in order`() {
        // Arrange
        every { mockLikeService.removeLike(1L, 10L) } returns Unit
        every { mockProductService.decrementLikeCount(10L) } just Runs

        // Act
        likeFacade.removeLike(userId = 1L, productId = 10L)

        // Assert
        verifyOrder {
            mockLikeService.removeLike(1L, 10L)
            mockProductService.decrementLikeCount(10L)
        }
    }

    @Test
    fun `removeLike() throws NOT_FOUND when like does not exist`() {
        // Arrange
        every { mockLikeService.removeLike(1L, 10L) } throws CoreException(ErrorType.NOT_FOUND, "좋아요 기록이 존재하지 않습니다.")

        // Act & Assert
        assertThrows<CoreException> {
            likeFacade.removeLike(userId = 1L, productId = 10L)
        }.also {
            assertThat(it.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        verify(exactly = 0) { mockProductService.decrementLikeCount(any()) }
    }

    // ─── getLikedProducts ───

    @Test
    fun `getLikedProducts() returns list of LikedProductResult for user`() {
        // Arrange
        val likes = listOf(createLike(userId = 1L, productId = 10L))
        val product = createProduct(id = 10L, brandId = 1L)
        val brand = createBrand(id = 1L, name = "Nike")
        every { mockLikeService.getLikedByUser(1L) } returns likes
        every { mockProductService.getById(10L) } returns product
        every { mockBrandService.getById(1L) } returns brand

        // Act
        val result = likeFacade.getLikedProducts(userId = 1L)

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result[0].productId).isEqualTo(10L)
        assertThat(result[0].brand.name).isEqualTo("Nike")
    }

    private fun createLike(id: Long = 0L, userId: Long = 1L, productId: Long = 10L): Like =
        Like(id = id, userId = userId, productId = productId)

    private fun createProduct(
        id: Long = 0L,
        brandId: Long = 1L,
        name: String = "Test Product",
        likeCount: Int = 0,
    ): Product = Product(id = id, brandId = brandId, name = name, description = "desc", price = 10000, stock = 100, likeCount = likeCount)

    private fun createBrand(id: Long = 0L, name: String = "TestBrand"): Brand =
        Brand(id = id, name = name, description = "desc")
}
