package com.loopers.domain.point

import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserPointTest {

    @Nested
    @DisplayName("UserPoint 생성 시")
    inner class Create {

        @Test
        @DisplayName("초기 잔액 0으로 생성된다")
        fun create_withDefaultBalance_success() {
            // act
            val userPoint = UserPoint(refUserId = 1L)

            // assert
            assertThat(userPoint.refUserId).isEqualTo(1L)
            assertThat(userPoint.balance).isEqualTo(0)
        }

        @Test
        @DisplayName("음수 잔액으로 생성하면 CoreException이 발생한다")
        fun create_withNegativeBalance_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                UserPoint(refUserId = 1L, balance = -100)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("charge 시")
    inner class Charge {

        @Test
        @DisplayName("충전하면 잔액이 증가한다")
        fun charge_increasesBalance() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)

            // act
            userPoint.charge(Point(5000))

            // assert
            assertThat(userPoint.balance).isEqualTo(5000)
        }

        @Test
        @DisplayName("충전 금액이 0이면 BAD_REQUEST 예외가 발생한다")
        fun charge_zeroAmount_throwsException() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                userPoint.charge(Point(0))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("충전 금액은 0보다 커야 합니다.")
        }

        @Test
        @DisplayName("충전 금액이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun charge_negativeAmount_throwsException() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                userPoint.charge(Point(-1000))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("여러 번 충전하면 잔액이 누적된다")
        fun charge_multiple_accumulatesBalance() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)

            // act
            userPoint.charge(Point(3000))
            userPoint.charge(Point(2000))

            // assert
            assertThat(userPoint.balance).isEqualTo(5000)
        }

        @Test
        @DisplayName("충전 후 잔액이 MAX_BALANCE를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun charge_exceedMaxBalance_throwsBadRequest() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(5_000_000))

            // act
            val exception = assertThrows<CoreException> {
                userPoint.charge(Point(6_000_000))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("최대 한도")
        }
    }

    @Nested
    @DisplayName("use 시")
    inner class Use {

        @Test
        @DisplayName("잔액이 충분하면 차감된다")
        fun use_sufficientBalance_decreasesBalance() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(10000))

            // act
            userPoint.use(Point(3000))

            // assert
            assertThat(userPoint.balance).isEqualTo(7000)
        }

        @Test
        @DisplayName("잔액이 부족하면 CoreException이 발생한다")
        fun use_insufficientBalance_throwsException() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(1000))

            // act
            val exception = assertThrows<CoreException> {
                userPoint.use(Point(5000))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("필요: 5000")
            assertThat(exception.message).contains("현재: 1000")
        }

        @Test
        @DisplayName("사용 금액이 0이면 BAD_REQUEST 예외가 발생한다")
        fun use_zeroAmount_throwsBadRequest() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(5000))

            // act
            val exception = assertThrows<CoreException> {
                userPoint.use(Point(0))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("사용 포인트는 0보다 커야 합니다.")
        }

        @Test
        @DisplayName("사용 금액이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun use_negativeAmount_throwsBadRequest() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(5000))

            // act
            val exception = assertThrows<CoreException> {
                userPoint.use(Point(-1000))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("canAfford 시")
    inner class CanAfford {

        @Test
        @DisplayName("잔액이 충분하면 true를 반환한다")
        fun canAfford_sufficientBalance_returnsTrue() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(5000))

            // act & assert
            assertThat(userPoint.canAfford(Point(5000))).isTrue()
        }

        @Test
        @DisplayName("잔액이 부족하면 false를 반환한다")
        fun canAfford_insufficientBalance_returnsFalse() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(Point(1000))

            // act & assert
            assertThat(userPoint.canAfford(Point(5000))).isFalse()
        }
    }
}
