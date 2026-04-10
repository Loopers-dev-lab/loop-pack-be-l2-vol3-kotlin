package com.loopers.interfaces.api.admin.queue

import com.loopers.application.queue.QueueStatusInfo

class AdminQueueV1Dto {

    data class ToggleRequest(
        val enabled: Boolean,
    )

    data class ToggleResponse(
        val enabled: Boolean,
    )

    data class StatusResponse(
        val enabled: Boolean,
        val totalWaiting: Long,
        val activeTokens: Long,
    ) {
        companion object {
            fun from(info: QueueStatusInfo) = StatusResponse(
                enabled = info.enabled,
                totalWaiting = info.totalWaiting,
                activeTokens = info.activeTokens,
            )
        }
    }
}
