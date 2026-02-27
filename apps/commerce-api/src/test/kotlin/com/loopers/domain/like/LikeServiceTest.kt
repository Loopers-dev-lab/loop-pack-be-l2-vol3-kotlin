package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LikeServiceTest {

    private lateinit var likeService: LikeService
    private lateinit var likeRepository: FakeLikeRepository

    @BeforeEach
    fun setUp() {
        likeRepository = FakeLikeRepository()
        likeService = LikeService(likeRepository)
    }

    @Nested
    inner class LikeProduct {

        @Test
        @DisplayName("좋아요를 등록하면 성공한다")
        fun success() {
            // arrange & act
            val result = likeService.like(userId = 1L, productId = 1L)

            // assert
            assertThat(result.likeId).isGreaterThan(0)
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.productId).isEqualTo(1L)
            assertThat(result.likedAt).isNotNull()
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 CONFLICT 예외가 발생한다")
        fun duplicateLikeThrowsConflict() {
            // arrange
            likeService.like(userId = 1L, productId = 1L)

            // act
            val result = assertThrows<CoreException> {
                likeService.like(userId = 1L, productId = 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @Nested
    inner class UnlikeProduct {

        @Test
        @DisplayName("좋아요를 취소하면 성공한다")
        fun success() {
            // arrange
            likeService.like(userId = 1L, productId = 1L)

            // act
            likeService.unlike(userId = 1L, productId = 1L)

            // assert
            val likes = likeService.findAllByUserId(1L)
            assertThat(likes).isEmpty()
        }

        @Test
        @DisplayName("좋아요하지 않은 상품을 취소하면 NOT_FOUND 예외가 발생한다")
        fun unlikeNonExistentThrowsNotFound() {
            // act
            val result = assertThrows<CoreException> {
                likeService.unlike(userId = 1L, productId = 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindAllByUserId {

        @Test
        @DisplayName("사용자의 좋아요 목록을 조회하면 성공한다")
        fun success() {
            // arrange
            likeService.like(userId = 1L, productId = 1L)
            likeService.like(userId = 1L, productId = 2L)
            likeService.like(userId = 2L, productId = 1L)

            // act
            val likes = likeService.findAllByUserId(1L)

            // assert
            assertThat(likes).hasSize(2)
            assertThat(likes.map { it.productId }).containsExactlyInAnyOrder(1L, 2L)
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 목록이 반환된다")
        fun emptyWhenNoLikes() {
            // act
            val likes = likeService.findAllByUserId(1L)

            // assert
            assertThat(likes).isEmpty()
        }
    }

    @Nested
    inner class DeleteAllByProductId {

        @Test
        @DisplayName("상품의 모든 좋아요가 삭제된다")
        fun success() {
            // arrange
            likeService.like(userId = 1L, productId = 1L)
            likeService.like(userId = 2L, productId = 1L)
            likeService.like(userId = 1L, productId = 2L)

            // act
            likeService.deleteAllByProductId(1L)

            // assert
            val user1Likes = likeService.findAllByUserId(1L)
            val user2Likes = likeService.findAllByUserId(2L)
            assertThat(user1Likes).hasSize(1)
            assertThat(user1Likes[0].productId).isEqualTo(2L)
            assertThat(user2Likes).isEmpty()
        }
    }
}
