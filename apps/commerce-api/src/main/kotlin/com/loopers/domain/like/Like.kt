package com.loopers.domain.like

import java.time.ZonedDateTime

class Like private constructor(
    val persistenceId: Long?,
    val refUserId: Long,
    val refProductId: Long,
    val createdAt: ZonedDateTime,
) {

    companion object {
        fun create(userId: Long, productId: Long): Like {
            return Like(
                persistenceId = null,
                refUserId = userId,
                refProductId = productId,
                createdAt = ZonedDateTime.now(),
            )
        }

        fun reconstitute(
            persistenceId: Long,
            refUserId: Long,
            refProductId: Long,
            createdAt: ZonedDateTime,
        ): Like {
            return Like(
                persistenceId = persistenceId,
                refUserId = refUserId,
                refProductId = refProductId,
                createdAt = createdAt,
            )
        }
    }
}
