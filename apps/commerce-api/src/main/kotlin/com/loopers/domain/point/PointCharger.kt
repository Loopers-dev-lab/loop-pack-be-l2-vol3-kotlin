package com.loopers.domain.point

import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.repository.PointHistoryRepository
import com.loopers.domain.point.repository.UserPointRepository
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class PointCharger(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {

    fun charge(userId: Long, amount: Point): UserPoint {
        if (amount.value > MAX_CHARGE_AMOUNT) {
            throw CoreException(ErrorType.BAD_REQUEST, "1회 충전 한도는 ${MAX_CHARGE_AMOUNT}포인트입니다.")
        }
        val userPoint = userPointRepository.findByUserIdForUpdate(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        userPoint.charge(amount)
        val savedUserPoint = userPointRepository.save(userPoint)
        pointHistoryRepository.save(
            PointHistory(
                refUserPointId = savedUserPoint.id,
                type = PointHistoryType.CHARGE,
                amount = amount,
            ),
        )
        return savedUserPoint
    }

    companion object {
        const val MAX_CHARGE_AMOUNT = 10_000_000L
    }
}
