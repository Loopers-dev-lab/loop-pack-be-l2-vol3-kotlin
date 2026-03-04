package com.loopers.application.user.like

import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import java.math.BigDecimal

@DisplayName("상품 좋아요 등록")
class UserProductLikeRegisterUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val productLikeRepository: ProductLikeRepository = mock()
    private val useCase = UserProductLikeRegisterUseCase(productRepository, productLikeRepository)

    private fun activeProduct(id: Long, brandId: Long = 1L): Product =
        Product.retrieve(
            id = id,
            name = "상품$id",
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("8000")),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = null,
            likeCount = 0,
            status = Product.Status.ACTIVE,
        )

    private fun inactiveProduct(id: Long, brandId: Long = 1L): Product =
        Product.retrieve(
            id = id,
            name = "상품$id",
            regularPrice = Money(BigDecimal("10000")),
            sellingPrice = Money(BigDecimal("8000")),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = null,
            likeCount = 0,
            status = Product.Status.INACTIVE,
        )

    @Nested
    @DisplayName("상품이 ACTIVE이면 좋아요 등록에 성공한다")
    inner class WhenProductActiveAndNotDeleted {
        @Test
        @DisplayName("정상적으로 좋아요가 등록된다")
        fun register_success() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 1L)
            given(productRepository.findById(eq(1L))).willReturn(activeProduct(1L))
            given(productLikeRepository.existsByUserIdAndProductId(eq(1L), eq(1L))).willReturn(false)

            useCase.register(command)

            then(productLikeRepository).should().save(
                check<ProductLike> {
                    assertThat(it.userId).isEqualTo(1L)
                    assertThat(it.productId).isEqualTo(1L)
                },
            )
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 실패한다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("존재하지 않는 상품 ID → PRODUCT_NOT_FOUND")
        fun register_productNotFound() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 999L)
            given(productRepository.findById(eq(999L))).willReturn(null)

            val exception = assertThrows<CoreException> {
                useCase.register(command)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("상품이 INACTIVE이면 실패한다")
    inner class WhenProductInactive {
        @Test
        @DisplayName("INACTIVE 상품 → PRODUCT_NOT_FOUND")
        fun register_productInactive() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 1L)
            given(productRepository.findById(eq(1L))).willReturn(inactiveProduct(1L))

            val exception = assertThrows<CoreException> {
                useCase.register(command)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("이미 좋아요가 등록되어 있으면 정상 반환한다 (멱등)")
    inner class WhenAlreadyLiked {
        @Test
        @DisplayName("이미 존재하는 좋아요 → save 미호출, 정상 반환")
        fun register_alreadyExists() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 1L)
            given(productRepository.findById(eq(1L))).willReturn(activeProduct(1L))
            given(productLikeRepository.existsByUserIdAndProductId(eq(1L), eq(1L))).willReturn(true)

            assertDoesNotThrow { useCase.register(command) }

            then(productLikeRepository).should(never()).save(check<ProductLike> {})
        }
    }
}
