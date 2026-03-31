package com.loopers.application.user.like

import com.loopers.domain.like.ProductLikeRepository
import com.loopers.support.event.user.ProductLikeCanceledEvent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.springframework.context.ApplicationEventPublisher
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

@DisplayName("상품 좋아요 취소")
class UserProductLikeCancelUseCaseTest {
    private val eventPublisher: ApplicationEventPublisher = mock()
    private val productLikeRepository: ProductLikeRepository = mock()
    private val useCase = UserProductLikeCancelUseCase(
        eventPublisher,
        productLikeRepository,
    )

    @Nested
    @DisplayName("좋아요가 존재하면 삭제에 성공한다")
    inner class WhenLikeExists {
        @Test
        @DisplayName("좋아요 삭제 → deleteByUserIdAndProductId 호출 + decrementLikeCount 호출")
        fun cancel_success() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.deleteByUserIdAndProductId(eq(1L), eq(1L))).willReturn(true)

            useCase.cancel(command)

            then(productLikeRepository).should().deleteByUserIdAndProductId(eq(1L), eq(1L))
            then(eventPublisher).should().publishEvent(
                check<ProductLikeCanceledEvent> { event ->
                    kotlin.test.assertEquals(1L, event.userId)
                    kotlin.test.assertEquals(1L, event.productId)
                },
            )
        }
    }

    @Nested
    @DisplayName("좋아요가 존재하지 않으면 정상 반환한다 (멱등)")
    inner class WhenLikeNotExists {
        @Test
        @DisplayName("존재하지 않는 좋아요 → 카운트 미감소, 정상 반환")
        fun cancel_notExists() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.deleteByUserIdAndProductId(eq(1L), eq(1L))).willReturn(false)

            assertDoesNotThrow { useCase.cancel(command) }

            then(productLikeRepository).should().deleteByUserIdAndProductId(eq(1L), eq(1L))
            then(eventPublisher).should(never()).publishEvent(check<ProductLikeCanceledEvent> { })
        }
    }

    @Nested
    @DisplayName("상품이 soft-delete 상태여도 좋아요 row가 있으면 삭제한다")
    inner class WhenProductSoftDeleted {
        @Test
        @DisplayName("soft-delete 상품 + like row 있음 → delete + decrementLikeCount 호출, findById 미호출")
        fun cancel_productSoftDeleted_likeRowExists() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.deleteByUserIdAndProductId(eq(1L), eq(1L))).willReturn(true)

            useCase.cancel(command)

            then(productLikeRepository).should().deleteByUserIdAndProductId(eq(1L), eq(1L))
            then(eventPublisher).should().publishEvent(check<ProductLikeCanceledEvent> { })
        }
    }

    @Nested
    @DisplayName("상품이 INACTIVE 상태여도 좋아요 row가 있으면 삭제한다")
    inner class WhenProductInactive {
        @Test
        @DisplayName("INACTIVE 상품 + like row 있음 → delete + decrementLikeCount 호출, findById 미호출")
        fun cancel_productInactive_likeRowExists() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.deleteByUserIdAndProductId(eq(1L), eq(1L))).willReturn(true)

            useCase.cancel(command)

            then(productLikeRepository).should().deleteByUserIdAndProductId(eq(1L), eq(1L))
            then(eventPublisher).should().publishEvent(check<ProductLikeCanceledEvent> { })
        }
    }
}
