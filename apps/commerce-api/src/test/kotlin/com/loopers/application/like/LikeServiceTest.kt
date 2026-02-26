package com.loopers.application.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LikeServiceTest {

    @Mock
    private lateinit var likeRepository: LikeRepository

    @InjectMocks
    private lateinit var likeService: LikeService

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class AddLike {

        @DisplayName("좋아요가 없으면, 신규 생성된다.")
        @Test
        fun createsNewLike_whenNoExistingLike() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(null)
            whenever(likeRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertAll(
                { assertThat(result.userId).isEqualTo(userId) },
                { assertThat(result.productId).isEqualTo(productId) },
            )
            verify(likeRepository).save(any())
        }

        @DisplayName("이미 활성 좋아요가 있으면, 기존 좋아요를 반환한다.")
        @Test
        fun returnsExistingLike_whenActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val existingLike = Like(userId = userId, productId = productId)

            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(existingLike)

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertThat(result).isSameAs(existingLike)
            verify(likeRepository, never()).save(any())
        }

        @DisplayName("Soft Delete된 좋아요가 있으면, 복원된다.")
        @Test
        fun restoresLike_whenSoftDeletedLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val deletedLike = Like(userId = userId, productId = productId)
            deletedLike.delete()

            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(deletedLike)
            whenever(likeRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertAll(
                { assertThat(result.isDeleted()).isFalse() },
                { assertThat(result.userId).isEqualTo(userId) },
            )
            verify(likeRepository).save(any())
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class CancelLike {

        @DisplayName("활성 좋아요가 있으면, Soft Delete된다.")
        @Test
        fun softDeletesLike_whenActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val like = Like(userId = userId, productId = productId)

            whenever(likeRepository.findActiveByUserIdAndProductId(userId, productId)).thenReturn(like)
            whenever(likeRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            likeService.cancelLike(userId, productId)

            // assert
            assertThat(like.isDeleted()).isTrue()
            verify(likeRepository).save(like)
        }

        @DisplayName("좋아요가 없으면, 아무 작업도 하지 않는다.")
        @Test
        fun doesNothing_whenNoActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.findActiveByUserIdAndProductId(userId, productId)).thenReturn(null)

            // act
            likeService.cancelLike(userId, productId)

            // assert
            verify(likeRepository, never()).save(any())
        }
    }

    @DisplayName("좋아요 목록을 조회할 때,")
    @Nested
    inner class GetUserLikes {

        @DisplayName("활성 좋아요 목록이 반환된다.")
        @Test
        fun returnsActiveLikes() {
            // arrange
            val userId = 1L
            val likes = listOf(
                Like(userId = userId, productId = 1L),
                Like(userId = userId, productId = 2L),
            )

            whenever(likeRepository.findAllActiveByUserId(userId)).thenReturn(likes)

            // act
            val result = likeService.getUserLikes(userId)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("좋아요가 없으면, 빈 목록이 반환된다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // arrange
            val userId = 1L

            whenever(likeRepository.findAllActiveByUserId(userId)).thenReturn(emptyList())

            // act
            val result = likeService.getUserLikes(userId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
