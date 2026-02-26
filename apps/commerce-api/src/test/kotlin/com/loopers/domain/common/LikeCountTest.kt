package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LikeCountTest {

    @DisplayName("LikeCount 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("값이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenValueIsNegative() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                LikeCount.of(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("0이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsLikeCount_whenZeroProvided() {
            // arrange & act
            val likeCount = LikeCount.of(0)

            // assert
            assertThat(likeCount.value).isEqualTo(0)
        }
    }

    @DisplayName("increment를 호출하면,")
    @Nested
    inner class Increment {

        @DisplayName("값이 1 증가한 LikeCount를 반환한다.")
        @Test
        fun returnsIncrementedLikeCount() {
            // arrange
            val likeCount = LikeCount.of(5)

            // act
            val result = likeCount.increment()

            // assert
            assertThat(result.value).isEqualTo(6)
        }
    }

    @DisplayName("decrement를 호출하면,")
    @Nested
    inner class Decrement {

        @DisplayName("값이 1 감소한 LikeCount를 반환한다.")
        @Test
        fun returnsDecrementedLikeCount() {
            // arrange
            val likeCount = LikeCount.of(5)

            // act
            val result = likeCount.decrement()

            // assert
            assertThat(result.value).isEqualTo(4)
        }

        @DisplayName("0이면, 0을 유지한다.")
        @Test
        fun remainsZero_whenAlreadyZero() {
            // arrange
            val likeCount = LikeCount.of(0)

            // act
            val result = likeCount.decrement()

            // assert
            assertThat(result.value).isEqualTo(0)
        }
    }

    @DisplayName("값 동등성을 비교할 때,")
    @Nested
    inner class Equality {

        @DisplayName("같은 값이면 동등하다.")
        @Test
        fun isEqual_whenSameValue() {
            // arrange
            val lc1 = LikeCount.of(10)
            val lc2 = LikeCount.of(10)

            // assert
            assertThat(lc1).isEqualTo(lc2)
            assertThat(lc1.hashCode()).isEqualTo(lc2.hashCode())
        }
    }
}
