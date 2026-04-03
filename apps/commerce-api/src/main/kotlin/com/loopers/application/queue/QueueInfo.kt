package com.loopers.application.queue

import java.time.ZonedDateTime

class QueueInfo {
    data class Status(
        val strategy: QueueStrategyType,
        val state: QueueEntryState,
        val position: Long?,
        val totalWaitingCount: Long,
        val expectedWaitSeconds: Long,
        val token: String?,
        val tokenExpiresAt: ZonedDateTime?,
    ) {
        val canEnterOrderApi: Boolean
            get() = state == QueueEntryState.ADMITTED && token != null
    }
}
