package com.loopers.application.like

import com.loopers.domain.Money
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.FakeBrandRepository
import com.loopers.domain.like.FakeLikeRepository
import com.loopers.domain.like.LikeService
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.FakeProductRepository
import com.loopers.domain.product.ProductService
import com.loopers.domain.event.DomainEventPublisher
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class LikeFacadeTest {

    private lateinit var likeFacade: LikeFacade
    private lateinit var likeService: LikeService
    private lateinit var productService: ProductService
    private lateinit var brandService: BrandService

    @BeforeEach
    fun setUp() {
        likeService = LikeService(FakeLikeRepository())
        productService = ProductService(FakeProductRepository())
        brandService = BrandService(FakeBrandRepository())
        likeFacade = LikeFacade(likeService, productService, brandService, Mockito.mock(DomainEventPublisher::class.java))
    }

    private fun createBrand(name: String = "나이키"): Long {
        return brandService.createBrand(CreateBrandCommand(name, null, null)).id
    }

    private fun createProduct(brandId: Long, name: String = "에어맥스 90"): Long {
        return productService.createProduct(
            CreateProductCommand(
                brandId = brandId,
                name = name,
                description = null,
                price = Money(139000),
                stockQuantity = 100,
                displayYn = true,
                imageUrl = null,
            ),
        ).id
    }

    @Nested
    inner class Like {

        @Test
        @DisplayName("좋아요를 등록하면 likeCount가 증가한다")
        fun success() {
            // arrange
            val brandId = createBrand()
            val productId = createProduct(brandId)

            // act
            likeFacade.like(userId = 1L, productId = productId)

            // assert
            val product = productService.findById(productId)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("여러 유저가 좋아요하면 likeCount가 누적된다")
        fun multipleUsers() {
            // arrange
            val brandId = createBrand()
            val productId = createProduct(brandId)

            // act
            likeFacade.like(userId = 1L, productId = productId)
            likeFacade.like(userId = 2L, productId = productId)
            likeFacade.like(userId = 3L, productId = productId)

            // assert
            val product = productService.findById(productId)
            assertThat(product.likeCount).isEqualTo(3)
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 CONFLICT 예외가 발생한다")
        fun duplicateLikeThrowsConflict() {
            // arrange
            val brandId = createBrand()
            val productId = createProduct(brandId)
            likeFacade.like(userId = 1L, productId = productId)

            // act
            val result = assertThrows<CoreException> {
                likeFacade.like(userId = 1L, productId = productId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("존재하지 않는 상품에 좋아요하면 NOT_FOUND 예외가 발생한다")
        fun nonExistentProductThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                likeFacade.like(userId = 1L, productId = 999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class Unlike {

        @Test
        @DisplayName("좋아요를 취소하면 likeCount가 감소한다")
        fun success() {
            // arrange
            val brandId = createBrand()
            val productId = createProduct(brandId)
            likeFacade.like(userId = 1L, productId = productId)
            likeFacade.like(userId = 2L, productId = productId)

            // act
            likeFacade.unlike(userId = 1L, productId = productId)

            // assert
            val product = productService.findById(productId)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요하지 않은 상품을 취소하면 NOT_FOUND 예외가 발생한다")
        fun unlikeNonExistentThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                likeFacade.unlike(userId = 1L, productId = 999L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class GetLikedProducts {

        @Test
        @DisplayName("좋아요한 상품 목록을 조회한다")
        fun success() {
            // arrange
            val brandId = createBrand()
            val productId1 = createProduct(brandId, "상품A")
            val productId2 = createProduct(brandId, "상품B")
            likeFacade.like(userId = 1L, productId = productId1)
            likeFacade.like(userId = 1L, productId = productId2)

            // act
            val result = likeFacade.getLikedProducts(userId = 1L)

            // assert
            assertThat(result).hasSize(2)
            assertThat(result.map { it.productName }).containsExactlyInAnyOrder("상품A", "상품B")
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 목록이 반환된다")
        fun emptyWhenNoLikes() {
            // act
            val result = likeFacade.getLikedProducts(userId = 1L)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
