package com.loopers.application.order

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.order.OrderAdminService
import com.loopers.domain.user.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetOrdersUseCase(
    private val orderAdminService: OrderAdminService,
    private val userService: UserService,
) : UseCase<ListOrdersCriteria, ListOrdersResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ListOrdersCriteria): ListOrdersResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = orderAdminService.getOrders(pageable)

        val userIds = slice.content.map { it.userId }.distinct()
        val usernameMap = userIds.associateWith { id ->
            userService.findUserById(id)?.username ?: ""
        }

        val sliceResult = SliceResult.from(slice) { info ->
            GetOrderResult.from(info, username = usernameMap[info.userId] ?: "")
        }
        return ListOrdersResult.from(sliceResult)
    }
}
