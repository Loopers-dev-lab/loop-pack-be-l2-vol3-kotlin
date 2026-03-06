package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("LikeFacade")
class LikeFacadeTest {

    private val likeService: LikeService = mockk()
    private val productService: ProductService = mockk()
    private val likeFacade = LikeFacade(likeService, productService)

    companion object {
        private const val USER_ID = 1L
        private const val PRODUCT_ID = 100L
    }

    private fun createProduct(): ProductModel = ProductModel(
        name = "감성 티셔츠",
        price = 25000L,
        brandId = 1L,
        likesCount = 5L,
    )

    @DisplayName("likeProduct")
    @Nested
    inner class LikeProduct {
        @DisplayName("존재하는 상품에 신규 좋아요를 등록하면 likesCount가 증가한다")
        @Test
        fun incrementsLikesCount_whenNewLikeOnExistingProduct() {
            // arrange
            val product = createProduct()
            every { productService.findById(PRODUCT_ID) } returns product
            every { likeService.like(USER_ID, PRODUCT_ID) } returns true
            every { productService.incrementLikesCount(PRODUCT_ID) } just runs

            // act
            likeFacade.likeProduct(USER_ID, PRODUCT_ID)

            // assert
            verify(exactly = 1) { productService.findById(PRODUCT_ID) }
            verify(exactly = 1) { likeService.like(USER_ID, PRODUCT_ID) }
            verify(exactly = 1) { productService.incrementLikesCount(PRODUCT_ID) }
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 likesCount가 변동되지 않는다")
        @Test
        fun doesNotIncrementLikesCount_whenAlreadyLiked() {
            // arrange
            val product = createProduct()
            every { productService.findById(PRODUCT_ID) } returns product
            every { likeService.like(USER_ID, PRODUCT_ID) } returns false

            // act
            likeFacade.likeProduct(USER_ID, PRODUCT_ID)

            // assert
            verify(exactly = 1) { productService.findById(PRODUCT_ID) }
            verify(exactly = 1) { likeService.like(USER_ID, PRODUCT_ID) }
            verify(exactly = 0) { productService.incrementLikesCount(any()) }
        }

        @DisplayName("존재하지 않는 상품에 좋아요하면 NOT_FOUND 예외가 발생한다")
        @Test
        fun throwsNotFoundException_whenProductDoesNotExist() {
            // arrange
            every { productService.findById(PRODUCT_ID) } throws
                CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다: $PRODUCT_ID")

            // act & assert
            assertThatThrownBy {
                likeFacade.likeProduct(USER_ID, PRODUCT_ID)
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND)

            verify(exactly = 1) { productService.findById(PRODUCT_ID) }
            verify(exactly = 0) { likeService.like(any(), any()) }
            verify(exactly = 0) { productService.incrementLikesCount(any()) }
        }
    }

    @DisplayName("unlikeProduct")
    @Nested
    inner class UnlikeProduct {
        @DisplayName("활성 좋아요를 취소하면 likesCount가 감소한다")
        @Test
        fun decrementsLikesCount_whenActiveLikeCancelled() {
            // arrange
            every { likeService.unlike(USER_ID, PRODUCT_ID) } returns true
            every { productService.decrementLikesCount(PRODUCT_ID) } just runs

            // act
            likeFacade.unlikeProduct(USER_ID, PRODUCT_ID)

            // assert
            verify(exactly = 1) { likeService.unlike(USER_ID, PRODUCT_ID) }
            verify(exactly = 1) { productService.decrementLikesCount(PRODUCT_ID) }
        }

        @DisplayName("좋아요 기록이 없거나 이미 취소된 경우 likesCount가 변동되지 않는다")
        @Test
        fun doesNotDecrementLikesCount_whenNoActiveLike() {
            // arrange
            every { likeService.unlike(USER_ID, PRODUCT_ID) } returns false

            // act
            likeFacade.unlikeProduct(USER_ID, PRODUCT_ID)

            // assert
            verify(exactly = 1) { likeService.unlike(USER_ID, PRODUCT_ID) }
            verify(exactly = 0) { productService.decrementLikesCount(any()) }
        }
    }
}
