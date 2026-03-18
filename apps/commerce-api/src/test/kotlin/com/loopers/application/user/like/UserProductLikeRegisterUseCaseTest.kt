package com.loopers.application.user.like

import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.domain.product.ProductRepository
import com.loopers.support.transaction.AfterCommitExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.Mockito.verifyNoInteractions
import java.math.BigDecimal

@DisplayName("상품 좋아요 등록")
class UserProductLikeRegisterUseCaseTest {
    private val afterCommitExecutor = AfterCommitExecutor { action -> action() }
    private val productQueryInvalidator: ProductQueryInvalidator = mock()
    private val productRepository: ProductRepository = mock()
    private val productLikeRepository: ProductLikeRepository = mock()
    private val useCase = UserProductLikeRegisterUseCase(
        afterCommitExecutor,
        productQueryInvalidator,
        productRepository,
        productLikeRepository,
    )

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
            given(
                productLikeRepository.save(
                    check<ProductLike> { it.userId == 1L && it.productId == 1L },
                ),
            ).willReturn(true)

            useCase.register(command)

            then(productLikeRepository).should().save(
                check<ProductLike> {
                    assertThat(it.userId).isEqualTo(1L)
                    assertThat(it.productId).isEqualTo(1L)
                },
            )
            then(productRepository).should().incrementLikeCount(eq(1L))
            then(productQueryInvalidator).should().invalidateDetails(eq(listOf(1L)))
        }
    }

    @Nested
    @DisplayName("이미 좋아요가 등록되어 있으면 no-op으로 처리한다 (멱등)")
    inner class WhenAlreadyLiked {
        @Test
        @DisplayName("중복 등록 시 카운트 미증가, 예외 없음")
        fun register_alreadyExists() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 1L)
            given(productRepository.findById(eq(1L))).willReturn(activeProduct(1L))
            given(
                productLikeRepository.save(
                    check<ProductLike> { it.userId == 1L && it.productId == 1L },
                ),
            ).willReturn(false)

            assertDoesNotThrow { useCase.register(command) }

            then(productLikeRepository).should().save(
                check<ProductLike> { it.userId == 1L && it.productId == 1L },
            )
            then(productRepository).should(never()).incrementLikeCount(eq(1L))
            then(productQueryInvalidator).should(never()).invalidateDetails(eq(listOf(1L)))
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 no-op으로 처리한다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("존재하지 않는 상품 ID → save 미호출, 예외 없음")
        fun register_productNotFound() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 999L)
            given(productRepository.findById(eq(999L))).willReturn(null)

            assertDoesNotThrow { useCase.register(command) }

            then(productLikeRepository).should(never()).save(
                check<ProductLike> { it.productId == 999L },
            )
            then(productRepository).should(never()).incrementLikeCount(eq(999L))
            verifyNoInteractions(productQueryInvalidator)
        }
    }

    @Nested
    @DisplayName("상품이 INACTIVE이면 no-op으로 처리한다")
    inner class WhenProductInactive {
        @Test
        @DisplayName("INACTIVE 상품 → save 미호출, 예외 없음")
        fun register_productInactive() {
            val command = UserProductLikeCommand.Register(userId = 1L, productId = 1L)
            given(productRepository.findById(eq(1L))).willReturn(inactiveProduct(1L))

            assertDoesNotThrow { useCase.register(command) }

            then(productLikeRepository).should(never()).save(
                check<ProductLike> { it.productId == 1L },
            )
            then(productRepository).should(never()).incrementLikeCount(eq(1L))
            verifyNoInteractions(productQueryInvalidator)
        }
    }
}
