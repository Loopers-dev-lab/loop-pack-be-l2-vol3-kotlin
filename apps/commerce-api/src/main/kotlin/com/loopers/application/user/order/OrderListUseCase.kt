package com.loopers.application.user.order

import com.loopers.domain.order.OrderRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class OrderListUseCase(
    private val orderRepository: OrderRepository,
) {
    @Transactional(readOnly = true)
    fun getList(
        userId: Long,
        from: LocalDate?,
        to: LocalDate?,
        pageRequest: PageRequest,
    ): PageResponse<OrderResult.ListItem> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val fromZdt = (from ?: today.minusMonths(1)).atStartOfDay(zone)
        val toZdt = (to ?: today).plusDays(1).atStartOfDay(zone)

        return orderRepository.findAllByUserId(userId, fromZdt, toZdt, pageRequest)
            .map { OrderResult.ListItem.from(it) }
    }
}
