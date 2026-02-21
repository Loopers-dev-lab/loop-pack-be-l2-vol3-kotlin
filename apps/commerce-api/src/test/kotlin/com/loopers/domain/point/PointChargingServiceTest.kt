package com.loopers.domain.point

import com.loopers.domain.point.entity.PointHistory.PointHistoryType
import com.loopers.domain.point.entity.UserPoint
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PointChargingServiceTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var pointChargingService: PointChargingService

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        pointChargingService = PointChargingService(userPointRepository, pointHistoryRepository)
    }

    private fun createUserPoint(userId: Long): UserPoint {
        return userPointRepository.save(UserPoint(refUserId = userId))
    }

    @Nested
    @DisplayName("charge 시")
    inner class Charge {

        @Test
        @DisplayName("충전하면 잔액이 증가하고 CHARGE 이력이 기록된다")
        fun charge_success_increasesBalanceAndRecordsHistory() {
            // arrange
            val userPoint = createUserPoint(1L)

            // act
            val result = pointChargingService.charge(1L, 5000)

            // assert
            assertThat(result.balance).isEqualTo(5000)

            val histories = pointHistoryRepository.findAllByUserPointId(userPoint.id)
            assertThat(histories).hasSize(1)
            assertThat(histories[0].type).isEqualTo(PointHistoryType.CHARGE)
            assertThat(histories[0].amount).isEqualTo(5000)
            assertThat(histories[0].refOrderId).isNull()
        }

        @Test
        @DisplayName("여러 번 충전하면 잔액과 이력이 누적된다")
        fun charge_multiple_accumulatesBalanceAndHistories() {
            // arrange
            val userPoint = createUserPoint(1L)

            // act
            pointChargingService.charge(1L, 3000)
            pointChargingService.charge(1L, 2000)

            // assert
            val updated = userPointRepository.findByUserId(1L)!!
            assertThat(updated.balance).isEqualTo(5000)

            val histories = pointHistoryRepository.findAllByUserPointId(userPoint.id)
            assertThat(histories).hasSize(2)
        }

        @Test
        @DisplayName("충전 금액이 0이면 BAD_REQUEST 예외가 발생한다")
        fun charge_zeroAmount_throwsBadRequest() {
            // arrange
            createUserPoint(1L)

            // act
            val exception = assertThrows<CoreException> {
                pointChargingService.charge(1L, 0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("충전 금액은 0보다 커야")
        }

        @Test
        @DisplayName("충전 금액이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun charge_negativeAmount_throwsBadRequest() {
            // arrange
            createUserPoint(1L)

            // act
            val exception = assertThrows<CoreException> {
                pointChargingService.charge(1L, -1000)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("충전 금액은 0보다 커야")
        }

        @Test
        @DisplayName("포인트 정보가 없으면 NOT_FOUND 예외가 발생한다")
        fun charge_noPointInfo_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                pointChargingService.charge(999L, 5000)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("1회 충전 한도(10,000,000)와 동일한 금액이면 충전에 성공한다")
        fun charge_exactMaxAmount_success() {
            // arrange
            createUserPoint(1L)

            // act
            val result = pointChargingService.charge(1L, 10_000_000)

            // assert
            assertThat(result.balance).isEqualTo(10_000_000)
        }

        @Test
        @DisplayName("1회 충전 한도(10,000,000)를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun charge_exceedMaxAmount_throwsBadRequest() {
            // arrange
            createUserPoint(1L)

            // act
            val exception = assertThrows<CoreException> {
                pointChargingService.charge(1L, 10_000_001)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("1회 충전 한도")
        }

        @Test
        @DisplayName("기존 잔액과 충전 금액의 합이 MAX_BALANCE를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun charge_exceedMaxBalance_throwsBadRequest() {
            // arrange
            createUserPoint(1L)
            pointChargingService.charge(1L, 5_000_000)

            // act
            val exception = assertThrows<CoreException> {
                pointChargingService.charge(1L, 6_000_000)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
