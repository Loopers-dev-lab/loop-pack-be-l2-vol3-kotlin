package com.loopers.domain.point

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PointHistoryTest {

    @Nested
    @DisplayName("PointHistory 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 금액으로 생성하면 성공한다")
        fun create_withValidAmount_success() {
            // act
            val history = PointHistory(
                refUserPointId = 1L,
                type = PointHistoryType.CHARGE,
                amount = Point(1000),
            )

            // assert
            assertThat(history.amount.value).isEqualTo(1000)
            assertThat(history.type).isEqualTo(PointHistoryType.CHARGE)
            assertThat(history.refUserPointId).isEqualTo(1L)
        }

        @Test
        @DisplayName("금액이 0이면 BAD_REQUEST 예외가 발생한다")
        fun create_withZeroAmount_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                PointHistory(
                    refUserPointId = 1L,
                    type = PointHistoryType.CHARGE,
                    amount = Point(0),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("금액이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNegativeAmount_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                PointHistory(
                    refUserPointId = 1L,
                    type = PointHistoryType.USE,
                    amount = Point(-1000),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("주문 참조 ID와 함께 생성할 수 있다")
        fun create_withRefOrderId_success() {
            // act
            val history = PointHistory(
                refUserPointId = 1L,
                type = PointHistoryType.USE,
                amount = Point(5000),
                refOrderId = OrderId(100),
            )

            // assert
            assertThat(history.refOrderId).isEqualTo(OrderId(100))
        }
    }
}
