package com.loopers.domain.like

import java.time.ZonedDateTime

class Like private constructor(
    val persistenceId: Long?,
    val userId: Long,
    val productId: Long,
    val createdAt: ZonedDateTime,
) {

    companion object {
        fun create(userId: Long, productId: Long): Like {
            return Like(
                persistenceId = null,
                userId = userId,
                productId = productId,
                createdAt = ZonedDateTime.now(),
            )
        }

        fun reconstitute(
            persistenceId: Long,
            userId: Long,
            productId: Long,
            createdAt: ZonedDateTime,
        ): Like {
            return Like(
                persistenceId = persistenceId,
                userId = userId,
                productId = productId,
                createdAt = createdAt,
            )
        }
    }
}
