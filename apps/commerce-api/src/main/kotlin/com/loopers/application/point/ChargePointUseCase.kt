package com.loopers.application.point

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.PointCharger
import com.loopers.domain.point.vo.Point
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ChargePointUseCase(private val pointCharger: PointCharger) {
    @Transactional
    fun execute(userId: Long, amount: Long): PointBalanceInfo {
        val userPoint = pointCharger.charge(UserId(userId), Point(amount))
        return PointBalanceInfo.from(userPoint)
    }
}
