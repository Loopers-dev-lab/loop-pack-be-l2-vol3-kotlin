package com.loopers.application.user.like

import com.loopers.domain.like.ProductLikeRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never

@DisplayName("상품 좋아요 취소")
class UserProductLikeCancelUseCaseTest {
    private val productLikeRepository: ProductLikeRepository = mock()
    private val useCase = UserProductLikeCancelUseCase(productLikeRepository)

    @Nested
    @DisplayName("좋아요가 존재하면 삭제에 성공한다")
    inner class WhenLikeExists {
        @Test
        @DisplayName("좋아요 삭제 → deleteByUserIdAndProductId 호출")
        fun cancel_success() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.existsByUserIdAndProductId(eq(1L), eq(1L))).willReturn(true)

            useCase.cancel(command)

            then(productLikeRepository).should().deleteByUserIdAndProductId(eq(1L), eq(1L))
        }
    }

    @Nested
    @DisplayName("좋아요가 존재하지 않으면 정상 반환한다 (멱등)")
    inner class WhenLikeNotExists {
        @Test
        @DisplayName("존재하지 않는 좋아요 → delete 미호출, 정상 반환")
        fun cancel_notExists() {
            val command = UserProductLikeCommand.Cancel(userId = 1L, productId = 1L)
            given(productLikeRepository.existsByUserIdAndProductId(eq(1L), eq(1L))).willReturn(false)

            assertDoesNotThrow { useCase.cancel(command) }

            then(productLikeRepository).should(never()).deleteByUserIdAndProductId(eq(1L), eq(1L))
        }
    }
}
