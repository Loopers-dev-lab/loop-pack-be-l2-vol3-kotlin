package com.loopers.application.orderqueue

import com.loopers.application.UseCase
import com.loopers.domain.orderqueue.OrderQueueService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component

@Component
class UserGetQueuePositionUseCase(
    private val userService: UserService,
    private val orderQueueService: OrderQueueService,
) : UseCase<GetQueuePositionCriteria, GetQueuePositionResult> {

    override fun execute(criteria: GetQueuePositionCriteria): GetQueuePositionResult {
        val user = userService.getUser(criteria.loginId)
        val info = orderQueueService.getPosition(user.id)
        return GetQueuePositionResult.from(info)
    }
}
