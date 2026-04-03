package com.loopers.interfaces.api.queue

import com.loopers.application.queue.QueuePositionInfo

class QueueDto {

    data class EnterQueueResponse(
        val position: Long,
        val estimatedWaitSeconds: Double,
        val totalSize: Long,
    ) {
        companion object {
            fun from(info: QueuePositionInfo): EnterQueueResponse {
                return EnterQueueResponse(
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    totalSize = info.totalSize,
                )
            }
        }
    }

    data class QueuePositionResponse(
        val position: Long,
        val estimatedWaitSeconds: Double,
        val totalSize: Long,
        val token: String?,
        val pollingIntervalMs: Long,
        val bypassed: Boolean,
    ) {
        companion object {
            fun from(info: QueuePositionInfo): QueuePositionResponse {
                return QueuePositionResponse(
                    position = info.position,
                    estimatedWaitSeconds = info.estimatedWaitSeconds,
                    totalSize = info.totalSize,
                    token = info.token,
                    pollingIntervalMs = info.pollingIntervalMs,
                    bypassed = info.bypassed,
                )
            }
        }
    }
}
