package com.loopers.application.point

import com.loopers.domain.point.FakePointHistoryRepository
import com.loopers.domain.point.FakeUserPointRepository
import com.loopers.domain.point.PointCharger
import com.loopers.domain.point.model.UserPoint
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ChargePointUseCaseTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var chargePointUseCase: ChargePointUseCase

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        val pointCharger = PointCharger(userPointRepository, pointHistoryRepository)
        chargePointUseCase = ChargePointUseCase(pointCharger)
    }

    private fun createUserPoint(userId: Long): UserPoint {
        return userPointRepository.save(UserPoint(refUserId = userId))
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("정상 충전 시 PointBalanceInfo가 반환된다")
        fun execute_validAmount_returnsPointBalanceInfo() {
            // arrange
            createUserPoint(1L)

            // act
            val result = chargePointUseCase.execute(1L, 5000)

            // assert
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.balance).isEqualTo(5000)
        }

        @Test
        @DisplayName("0 포인트 충전 시 BAD_REQUEST 예외가 발생한다")
        fun execute_zeroAmount_throwsBadRequest() {
            // arrange
            createUserPoint(1L)

            // act
            val exception = assertThrows<CoreException> {
                chargePointUseCase.execute(1L, 0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("1회 충전 한도 초과 시 BAD_REQUEST 예외가 발생한다")
        fun execute_exceedMaxAmount_throwsBadRequest() {
            // arrange
            createUserPoint(1L)

            // act
            val exception = assertThrows<CoreException> {
                chargePointUseCase.execute(1L, 10_000_001)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("존재하지 않는 userId로 충전하면 NOT_FOUND 예외가 발생한다")
        fun execute_nonExistentUser_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                chargePointUseCase.execute(999L, 5000)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
