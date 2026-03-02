package com.loopers.domain.point

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PointDeductorTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var pointDeductor: PointDeductor

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        pointDeductor = PointDeductor(userPointRepository, pointHistoryRepository)
    }

    @Nested
    @DisplayName("usePoints 시")
    inner class UsePoints {

        @Test
        @DisplayName("잔액이 충분하면 차감되고 USE 이력이 기록된다")
        fun usePoints_sufficientBalance_deductsAndRecordsHistory() {
            // arrange
            val userPoint = userPointRepository.save(UserPoint(refUserId = UserId(1)))
            userPoint.charge(Point(10000))
            userPointRepository.save(userPoint)

            // act
            pointDeductor.usePoints(UserId(1), Money(BigDecimal("3000")), OrderId(100))

            // assert
            val updated = userPointRepository.findByUserId(UserId(1))!!
            assertThat(updated.balance.value).isEqualTo(7000)

            val histories = pointHistoryRepository.findAllByUserPointId(userPoint.id)
            assertThat(histories).hasSize(1)
            assertThat(histories[0].type).isEqualTo(PointHistoryType.USE)
            assertThat(histories[0].amount.value).isEqualTo(3000)
            assertThat(histories[0].refOrderId).isEqualTo(OrderId(100))
        }

        @Test
        @DisplayName("잔액이 부족하면 CoreException이 발생한다")
        fun usePoints_insufficientBalance_throwsException() {
            // arrange
            val userPoint = userPointRepository.save(UserPoint(refUserId = UserId(1)))
            userPoint.charge(Point(1000))
            userPointRepository.save(userPoint)

            // act
            val exception = assertThrows<CoreException> {
                pointDeductor.usePoints(UserId(1), Money(BigDecimal("5000")), OrderId(100))
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
                pointDeductor.usePoints(UserId(999), Money(BigDecimal("1000")), OrderId(100))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
