package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class LikeTest {

    @DisplayName("좋아요를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 정보가 주어지면, 좋아요가 생성된다.")
        @Test
        fun createsLike_whenValidInfoProvided() {
            // arrange
            val userId = 1L
            val productId = 1L

            // act
            val like = Like(userId = userId, productId = productId)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(userId) },
                { assertThat(like.productId).isEqualTo(productId) },
            )
        }

        @DisplayName("userId가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenUserIdIsZeroOrLess() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Like(userId = 0L, productId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("productId가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenProductIdIsZeroOrLess() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Like(userId = 1L, productId = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("좋아요를 삭제할 때,")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면, deletedAt이 설정된다.")
        @Test
        fun setDeletedAt_whenDeleteCalled() {
            // arrange
            val like = Like(userId = 1L, productId = 1L)

            // act
            like.delete()

            // assert
            assertThat(like.isDeleted()).isTrue()
        }

        @DisplayName("이미 삭제된 좋아요를 다시 삭제해도, 정상 동작한다.")
        @Test
        fun remainsDeleted_whenAlreadyDeleted() {
            // arrange
            val like = Like(userId = 1L, productId = 1L)
            like.delete()

            // act
            like.delete()

            // assert
            assertThat(like.isDeleted()).isTrue()
        }

        @DisplayName("삭제되지 않은 좋아요는 isDeleted가 false이다.")
        @Test
        fun returnsFalse_whenNotDeleted() {
            // arrange
            val like = Like(userId = 1L, productId = 1L)

            // act & assert
            assertThat(like.isDeleted()).isFalse()
        }
    }

    @DisplayName("좋아요를 복원할 때,")
    @Nested
    inner class Restore {

        @DisplayName("삭제된 좋아요를 복원하면, deletedAt이 null이 된다.")
        @Test
        fun restoresLike_whenDeletedLikeRestored() {
            // arrange
            val like = Like(userId = 1L, productId = 1L)
            like.delete()

            // act
            like.restore()

            // assert
            assertThat(like.isDeleted()).isFalse()
        }

        @DisplayName("삭제되지 않은 좋아요를 복원해도, 정상 동작한다.")
        @Test
        fun remainsActive_whenNotDeleted() {
            // arrange
            val like = Like(userId = 1L, productId = 1L)

            // act
            like.restore()

            // assert
            assertThat(like.isDeleted()).isFalse()
        }
    }
}
