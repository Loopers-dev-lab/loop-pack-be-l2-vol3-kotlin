package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueueEntryInfo

data class QueueEntryResponse(
    val position: Long,
    val estimatedWaitSeconds: Long,
    val alreadyHasToken: Boolean,
) {
    companion object {
        fun from(info: QueueEntryInfo): QueueEntryResponse = QueueEntryResponse(
            position = info.position,
            estimatedWaitSeconds = info.estimatedWaitSeconds,
            alreadyHasToken = info.alreadyHasToken,
        )
    }
}
