package com.loopers.domain.outbox.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.util.UUID

class OrderOutbox(
    val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long? = null,
    val reason: String? = null,
    val productId: Long? = null,
    val quantity: Int? = null,
    published: Boolean = false,
) {

    var published: Boolean = published
        private set

    init {
        if (eventType.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventType은 필수입니다.")
        if (orderId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "orderId는 양수여야 합니다.")
        if (userId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
        if (eventType == OrderOutboxEventType.PAYMENT_COMPLETED.name) {
            requireNotNull(productId) { "PAYMENT_COMPLETED 이벤트는 productId가 필수입니다." }
            requireNotNull(quantity) { "PAYMENT_COMPLETED 이벤트는 quantity가 필수입니다." }
        }
    }

    fun markPublished() {
        published = true
    }
}
