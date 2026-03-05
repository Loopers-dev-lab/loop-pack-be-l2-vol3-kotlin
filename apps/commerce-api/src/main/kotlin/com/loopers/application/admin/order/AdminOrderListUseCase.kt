package com.loopers.application.admin.order

import com.loopers.domain.order.AdminOrderRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class AdminOrderListUseCase(
    private val adminOrderRepository: AdminOrderRepository,
) {
    @Transactional(readOnly = true)
    fun getList(
        from: LocalDate?,
        to: LocalDate?,
        pageRequest: PageRequest,
    ): PageResponse<AdminOrderResult.ListItem> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val fromZdt = (from ?: today.minusMonths(1)).atStartOfDay(zone)
        val toZdt = (to ?: today).plusDays(1).atStartOfDay(zone)

        return adminOrderRepository.findAll(fromZdt, toZdt, pageRequest)
            .map { AdminOrderResult.ListItem.from(it) }
    }
}
