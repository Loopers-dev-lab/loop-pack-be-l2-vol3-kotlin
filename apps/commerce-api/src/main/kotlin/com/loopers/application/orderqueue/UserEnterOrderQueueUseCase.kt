package com.loopers.application.orderqueue

import com.loopers.application.UseCase
import com.loopers.domain.orderqueue.OrderQueueService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserEnterOrderQueueUseCase(
    private val userService: UserService,
    private val orderQueueService: OrderQueueService,
) : UseCase<EnterQueueCriteria, EnterQueueResult> {

    override fun execute(criteria: EnterQueueCriteria): EnterQueueResult {
        val user = userService.getUser(criteria.loginId)
        val info = orderQueueService.enter(user.id)
        return EnterQueueResult.from(info)
    }
}
