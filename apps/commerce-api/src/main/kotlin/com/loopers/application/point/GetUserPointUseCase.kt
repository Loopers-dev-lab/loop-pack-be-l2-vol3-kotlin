package com.loopers.application.point

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.repository.UserPointRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetUserPointUseCase(private val userPointRepository: UserPointRepository) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): PointBalanceInfo {
        val userPoint = userPointRepository.findByUserId(UserId(userId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다.")
        return PointBalanceInfo.from(userPoint)
    }
}
