package com.loopers.domain.point

import com.loopers.domain.point.entity.PointHistory.PointHistoryType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserPointServiceTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var userPointService: UserPointService

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        userPointService = UserPointService(userPointRepository, pointHistoryRepository)
    }

    @Nested
    @DisplayName("createUserPoint 시")
    inner class CreateUserPoint {

        @Test
        @DisplayName("초기 잔액 0으로 UserPoint가 생성된다")
        fun createUserPoint_createsWithZeroBalance() {
            // act
            val result = userPointService.createUserPoint(1L)

            // assert
            assertThat(result.refUserId).isEqualTo(1L)
            assertThat(result.balance).isEqualTo(0)
            assertThat(result.id).isNotEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("getBalance 시")
    inner class GetBalance {

        @Test
        @DisplayName("존재하는 사용자의 포인트를 조회하면 UserPoint가 반환된다")
        fun getBalance_existingUser_returnsUserPoint() {
            // arrange
            userPointService.createUserPoint(1L)

            // act
            val result = userPointService.getBalance(1L)

            // assert
            assertThat(result.refUserId).isEqualTo(1L)
            assertThat(result.balance).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 조회하면 NOT_FOUND 예외가 발생한다")
        fun getBalance_nonExistentUser_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                userPointService.getBalance(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("usePoints 시")
    inner class UsePoints {

        @Test
        @DisplayName("잔액이 충분하면 차감되고 USE 이력이 기록된다")
        fun usePoints_sufficientBalance_deductsAndRecordsHistory() {
            // arrange
            val userPoint = userPointService.createUserPoint(1L)
            userPoint.charge(10000)

            // act
            userPointService.usePoints(1L, 3000, 100L)

            // assert
            val updated = userPointService.getBalance(1L)
            assertThat(updated.balance).isEqualTo(7000)

            val histories = pointHistoryRepository.findAllByUserPointId(userPoint.id)
            assertThat(histories).hasSize(1)
            assertThat(histories[0].type).isEqualTo(PointHistoryType.USE)
            assertThat(histories[0].amount).isEqualTo(3000)
            assertThat(histories[0].refOrderId).isEqualTo(100L)
        }

        @Test
        @DisplayName("잔액이 부족하면 CoreException이 발생한다")
        fun usePoints_insufficientBalance_throwsException() {
            // arrange
            val userPoint = userPointService.createUserPoint(1L)
            userPoint.charge(1000)

            // act
            val exception = assertThrows<CoreException> {
                userPointService.usePoints(1L, 5000, 100L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("필요: 5000")
            assertThat(exception.message).contains("현재: 1000")
        }

        @Test
        @DisplayName("포인트 정보가 없으면 NOT_FOUND 예외가 발생한다")
        fun usePoints_noPointInfo_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                userPointService.usePoints(999L, 1000, 100L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
