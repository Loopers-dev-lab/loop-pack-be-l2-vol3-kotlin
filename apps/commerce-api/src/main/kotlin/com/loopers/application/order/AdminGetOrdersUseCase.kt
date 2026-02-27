package com.loopers.application.order

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
) : UseCase<ListOrdersCriteria, ListOrdersResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: ListOrdersCriteria): ListOrdersResult {
        val pageable = PageRequest.of(criteria.page, criteria.size)
        val slice = orderRepository.findAll(pageable)

        val userIds = slice.content.map { it.userId }.distinct()
        val usernameMap = userIds.associateWith { id ->
            userRepository.find(id)?.username ?: ""
        }

        val sliceResult = SliceResult.from(slice) { model ->
            val info = OrderInfo.from(model)
            GetOrderResult.from(info, username = usernameMap[model.userId] ?: "")
        }
        return ListOrdersResult.from(sliceResult)
    }
}
