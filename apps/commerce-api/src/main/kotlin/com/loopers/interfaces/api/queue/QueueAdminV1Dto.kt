package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueStatusResult

class QueueAdminV1Dto {

    data class QueueStatusResponse(
        val enabled: Boolean,
    ) {
        companion object {
            fun from(result: QueueStatusResult): QueueStatusResponse =
                QueueStatusResponse(enabled = result.enabled)
        }
    }
}
