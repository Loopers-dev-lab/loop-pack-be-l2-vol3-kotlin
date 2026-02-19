package com.loopers.domain.point

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
            userPoint.charge(5000)

            // assert
            assertThat(userPoint.balance).isEqualTo(5000)
        }

        @Test
        @DisplayName("여러 번 충전하면 잔액이 누적된다")
        fun charge_multiple_accumulatesBalance() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)

            // act
            userPoint.charge(3000)
            userPoint.charge(2000)

            // assert
            assertThat(userPoint.balance).isEqualTo(5000)
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
            userPoint.charge(10000)

            // act
            userPoint.use(3000)

            // assert
            assertThat(userPoint.balance).isEqualTo(7000)
        }

        @Test
        @DisplayName("잔액이 부족하면 CoreException이 발생한다")
        fun use_insufficientBalance_throwsException() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(1000)

            // act
            val exception = assertThrows<CoreException> {
                userPoint.use(5000)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("필요: 5000")
            assertThat(exception.message).contains("현재: 1000")
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
            userPoint.charge(5000)

            // act & assert
            assertThat(userPoint.canAfford(5000)).isTrue()
        }

        @Test
        @DisplayName("잔액이 부족하면 false를 반환한다")
        fun canAfford_insufficientBalance_returnsFalse() {
            // arrange
            val userPoint = UserPoint(refUserId = 1L)
            userPoint.charge(1000)

            // act & assert
            assertThat(userPoint.canAfford(5000)).isFalse()
        }
    }
}
