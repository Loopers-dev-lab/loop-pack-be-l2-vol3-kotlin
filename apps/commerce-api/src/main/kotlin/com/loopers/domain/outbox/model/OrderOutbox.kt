package com.loopers.domain.outbox.model

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.Quantity
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.util.UUID

class OrderOutbox(
    val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: OrderOutboxEventType,
    val orderId: OrderId,
    val userId: UserId,
    val totalAmount: Money? = null,
    val reason: String? = null,
    val productId: ProductId? = null,
    val quantity: Quantity? = null,
    published: Boolean = false,
) {

    var published: Boolean = published
        private set

    enum class OrderOutboxEventType {
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
    }

    init {
        if (orderId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "orderId는 양수여야 합니다.")
        if (userId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
        if (eventType == OrderOutboxEventType.PAYMENT_COMPLETED) {
            totalAmount ?: throw CoreException(ErrorType.BAD_REQUEST, "PAYMENT_COMPLETED 이벤트는 totalAmount가 필수입니다.")
            productId ?: throw CoreException(ErrorType.BAD_REQUEST, "PAYMENT_COMPLETED 이벤트는 productId가 필수입니다.")
            quantity ?: throw CoreException(ErrorType.BAD_REQUEST, "PAYMENT_COMPLETED 이벤트는 quantity가 필수입니다.")
        }
    }

    fun markPublished() {
        published = true
    }
}
