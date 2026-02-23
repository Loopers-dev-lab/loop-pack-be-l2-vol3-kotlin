package com.loopers.domain.point

import com.loopers.domain.point.entity.PointHistory
import com.loopers.domain.point.entity.PointHistory.PointHistoryType
import com.loopers.domain.point.entity.UserPoint
import com.loopers.domain.point.repository.PointHistoryRepository
import com.loopers.domain.point.repository.UserPointRepository
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
            throw CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.")
        }
        if (amount > MAX_CHARGE_AMOUNT) {
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
