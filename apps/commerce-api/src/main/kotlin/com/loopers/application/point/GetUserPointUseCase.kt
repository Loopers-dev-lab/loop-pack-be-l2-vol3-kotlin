package com.loopers.application.point

import com.loopers.domain.point.UserPointService
import org.springframework.stereotype.Component

@Component
class GetUserPointUseCase(private val userPointService: UserPointService) {
    fun execute(userId: Long): PointBalanceInfo {
        val userPoint = userPointService.getBalance(userId)
        return PointBalanceInfo.from(userPoint)
    }
}
