package com.loopers.domain.outbox.model

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.util.UUID

class CatalogOutbox(
    val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: CatalogOutboxEventType,
    val productId: ProductId,
    val userId: UserId?,
    published: Boolean = false,
) {

    var published: Boolean = published
        private set

    enum class CatalogOutboxEventType {
        LIKE_ADDED,
        LIKE_REMOVED,
        PRODUCT_VIEWED,
    }

    init {
        if (eventId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventId는 필수입니다.")
        if (productId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "productId는 양수여야 합니다.")
        if (eventType in listOf(CatalogOutboxEventType.LIKE_ADDED, CatalogOutboxEventType.LIKE_REMOVED)) {
            if (userId == null) throw CoreException(ErrorType.BAD_REQUEST, "${eventType.name} 이벤트는 userId가 필수입니다.")
            if (userId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
        } else if (userId != null && userId.value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "userId는 양수여야 합니다.")
        }
    }

    fun markPublished() {
        published = true
    }
}
