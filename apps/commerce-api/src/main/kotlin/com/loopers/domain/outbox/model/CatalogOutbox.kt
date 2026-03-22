package com.loopers.domain.outbox.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.util.UUID

class CatalogOutbox(
    val id: Long = 0,
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val productId: Long,
    val userId: Long?,
    var published: Boolean = false,
) {

    init {
        if (eventType.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "eventType은 필수입니다.")
        if (productId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "productId는 양수여야 합니다.")
    }

    fun markPublished() {
        published = true
    }
}
