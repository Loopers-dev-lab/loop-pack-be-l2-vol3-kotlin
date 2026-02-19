package com.loopers.domain.point

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PointTest {

    @Nested
    @DisplayName("Point 생성 시")
    inner class Create {

        @Test
        @DisplayName("0 이상의 값으로 생성하면 정상 생성된다")
        fun create_withValidValue_success() {
            // act
            val point = Point(1000)

            // assert
            assertThat(point.value).isEqualTo(1000)
        }

        @Test
        @DisplayName("0으로 생성하면 정상 생성된다")
        fun create_withZero_success() {
            // act
            val point = Point(0)

            // assert
            assertThat(point.value).isEqualTo(0)
        }

        @Test
        @DisplayName("음수로 생성하면 CoreException이 발생한다")
        fun create_withNegative_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                Point(-1)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("plus 연산 시")
    inner class Plus {

        @Test
        @DisplayName("두 포인트를 더하면 합산된 포인트가 반환된다")
        fun plus_returnsSummedPoint() {
            // arrange
            val point1 = Point(1000)
            val point2 = Point(500)

            // act
            val result = point1.plus(point2)

            // assert
            assertThat(result.value).isEqualTo(1500)
        }
    }

    @Nested
    @DisplayName("minus 연산 시")
    inner class Minus {

        @Test
        @DisplayName("차감 가능하면 차감된 포인트가 반환된다")
        fun minus_sufficientBalance_returnsSubtractedPoint() {
            // arrange
            val point1 = Point(1000)
            val point2 = Point(300)

            // act
            val result = point1.minus(point2)

            // assert
            assertThat(result.value).isEqualTo(700)
        }

        @Test
        @DisplayName("결과가 음수이면 CoreException이 발생한다")
        fun minus_insufficientBalance_throwsException() {
            // arrange
            val point1 = Point(100)
            val point2 = Point(500)

            // act
            val exception = assertThrows<CoreException> {
                point1.minus(point2)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
