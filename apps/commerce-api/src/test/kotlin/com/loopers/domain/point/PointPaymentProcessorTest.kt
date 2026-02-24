package com.loopers.domain.point

import com.loopers.domain.common.Money
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class PointPaymentProcessorTest {

    private lateinit var userPointRepository: FakeUserPointRepository
    private lateinit var pointHistoryRepository: FakePointHistoryRepository
    private lateinit var pointPaymentProcessor: PointPaymentProcessor

    @BeforeEach
    fun setUp() {
        userPointRepository = FakeUserPointRepository()
        pointHistoryRepository = FakePointHistoryRepository()
        pointPaymentProcessor = PointPaymentProcessor(userPointRepository, pointHistoryRepository)
    }

    @Nested
    @DisplayName("usePoints 시")
    inner class UsePoints {

        @Test
        @DisplayName("잔액이 충분하면 차감되고 USE 이력이 기록된다")
        fun usePoints_sufficientBalance_deductsAndRecordsHistory() {
            // arrange
            val userPoint = userPointRepository.save(com.loopers.domain.point.model.UserPoint(refUserId = 1L))
            userPoint.charge(10000)
            userPointRepository.save(userPoint)

            // act
            pointPaymentProcessor.usePoints(1L, Money(BigDecimal("3000")), 100L)

            // assert
            val updated = userPointRepository.findByUserId(1L)!!
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
            val userPoint = userPointRepository.save(com.loopers.domain.point.model.UserPoint(refUserId = 1L))
            userPoint.charge(1000)
            userPointRepository.save(userPoint)

            // act
            val exception = assertThrows<CoreException> {
                pointPaymentProcessor.usePoints(1L, Money(BigDecimal("5000")), 100L)
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
                pointPaymentProcessor.usePoints(999L, Money(BigDecimal("1000")), 100L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
