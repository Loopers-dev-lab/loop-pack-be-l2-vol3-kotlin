package com.loopers.application.point

import com.loopers.domain.point.PointChargingService
import org.springframework.stereotype.Component

@Component
class ChargePointUseCase(private val pointChargingService: PointChargingService) {
    fun execute(userId: Long, amount: Long): PointBalanceInfo {
        val userPoint = pointChargingService.charge(userId, amount)
        return PointBalanceInfo.from(userPoint)
    }
}
