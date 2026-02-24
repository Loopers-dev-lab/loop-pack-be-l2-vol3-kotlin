package com.loopers.domain.point

import com.loopers.domain.common.Money
import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.domain.point.repository.PointHistoryRepository
import com.loopers.domain.point.repository.UserPointRepository
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PointDeductor(
    private val userPointRepository: UserPointRepository,
    private val pointHistoryRepository: PointHistoryRepository,
) {

    @Transactional
    fun usePoints(userId: Long, amount: Money, refOrderId: Long) {
        val pointAmount = Point(amount.toLong())
        val userPoint = userPointRepository.findByUserIdForUpdate(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        userPoint.use(pointAmount)
        val savedUserPoint = userPointRepository.save(userPoint)
        pointHistoryRepository.save(
            PointHistory(
                refUserPointId = savedUserPoint.id,
                type = PointHistoryType.USE,
                amount = pointAmount,
                refOrderId = refOrderId,
            ),
        )
    }
}
