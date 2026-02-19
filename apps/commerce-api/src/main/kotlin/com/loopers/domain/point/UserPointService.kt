package com.loopers.domain.point

import com.loopers.domain.point.entity.PointHistory
import com.loopers.domain.point.entity.PointHistoryType
import com.loopers.domain.point.entity.UserPoint
import com.loopers.domain.point.repository.PointHistoryRepository
import com.loopers.domain.point.repository.UserPointRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserPointService(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {

    @Transactional
    fun createUserPoint(userId: Long): UserPoint {
        return userPointRepository.save(UserPoint(refUserId = userId))
    }

    @Transactional(readOnly = true)
    fun getBalance(userId: Long): UserPoint {
        return userPointRepository.findByUserId(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
    }

    @Transactional
    fun usePoints(userId: Long, amount: Long, refOrderId: Long) {
        val userPoint = userPointRepository.findByUserId(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        userPoint.use(amount)
        pointHistoryRepository.save(
            PointHistory(
                refUserPointId = userPoint.id,
                type = PointHistoryType.USE,
                amount = amount,
                refOrderId = refOrderId,
            ),
        )
    }
}
