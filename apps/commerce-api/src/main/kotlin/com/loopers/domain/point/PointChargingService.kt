package com.loopers.domain.point

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PointChargingService(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {

    @Transactional
    fun charge(userId: Long, amount: Long): UserPoint {
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 금액은 1 이상이어야 합니다.")
        }
        val userPoint = userPointRepository.findByUserId(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        userPoint.charge(amount)
        pointHistoryRepository.save(
            PointHistory(
                refUserPointId = userPoint.id,
                type = PointHistoryType.CHARGE,
                amount = amount,
            ),
        )
        return userPoint
    }
}
