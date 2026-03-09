package com.loopers.application.order

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.order.OrderService
import com.loopers.domain.user.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Component
class UserGetOrdersUseCase(
    private val orderService: OrderService,
    private val userService: UserService,
) : UseCase<GetOrdersCriteria, UserGetOrdersResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: GetOrdersCriteria): UserGetOrdersResult {
        val user = userService.getUser(criteria.loginId)
        val zoneId = ZoneId.of("Asia/Seoul")
        val startAt = criteria.startAt.atStartOfDay(zoneId)
        val endAt = criteria.endAt.plusDays(1).atStartOfDay(zoneId)
        val pageable = PageRequest.of(criteria.page, criteria.size)

        val orderSlice = orderService.getOrders(user.id, startAt, endAt, pageable)
        val sliceResult = SliceResult.from(orderSlice) { info ->
            UserGetOrderResult.from(info)
        }
        return UserGetOrdersResult.from(sliceResult)
    }
}
