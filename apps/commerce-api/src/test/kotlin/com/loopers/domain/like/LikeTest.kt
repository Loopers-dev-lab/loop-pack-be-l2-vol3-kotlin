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

    @DisplayName("좋아요 생성할 때,")
    @Nested
    inner class Create {
        private val userId = 1L
        private val productId = 1L

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsLike_whenValidValuesProvided() {
            // arrange & act
            val like = Like(userId = userId, productId = productId)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(userId) },
                { assertThat(like.productId).isEqualTo(productId) },
            )
        }

        @DisplayName("사용자 ID가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenUserIdIsNotPositive() {
            // act
            val exception = assertThrows<CoreException> {
                Like(userId = 0L, productId = productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("상품 ID가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenProductIdIsNotPositive() {
            // act
            val exception = assertThrows<CoreException> {
                Like(userId = userId, productId = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
