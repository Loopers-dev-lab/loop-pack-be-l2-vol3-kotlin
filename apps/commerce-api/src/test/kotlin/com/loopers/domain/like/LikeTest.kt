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

    private fun createLike(
        userId: Long = 1L,
        productId: Long = 1L,
    ): Like = Like(
        userId = userId,
        productId = productId,
    )

    @Nested
    inner class CreateLike {

        @Test
        @DisplayName("유효한 userId와 productId로 좋아요를 생성하면 성공한다")
        fun success() {
            // arrange & act
            val like = createLike(userId = 1L, productId = 2L)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(1L) },
                { assertThat(like.productId).isEqualTo(2L) },
            )
        }

        @Test
        @DisplayName("userId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun userIdZeroThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createLike(userId = 0L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("userId가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun userIdNegativeThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createLike(userId = -1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("productId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun productIdZeroThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createLike(productId = 0L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("productId가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun productIdNegativeThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createLike(productId = -1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
