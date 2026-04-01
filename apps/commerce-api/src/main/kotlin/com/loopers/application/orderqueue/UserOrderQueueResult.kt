package com.loopers.application.orderqueue

import com.loopers.domain.orderqueue.QueueEntryInfo
import com.loopers.domain.orderqueue.QueuePositionInfo
import com.loopers.domain.orderqueue.QueueStatus

data class EnterQueueResult(
    val position: Long,
    val totalWaiting: Long,
    val estimatedWaitSeconds: Long,
    val pollingIntervalSeconds: Int,
) {
    companion object {
        fun from(info: QueueEntryInfo): EnterQueueResult {
            return EnterQueueResult(
                position = info.position,
                totalWaiting = info.totalWaiting,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                pollingIntervalSeconds = info.pollingIntervalSeconds,
            )
        }
    }
}

data class GetQueuePositionResult(
    val status: QueueStatus,
    val position: Long? = null,
    val totalWaiting: Long? = null,
    val estimatedWaitSeconds: Long? = null,
    val pollingIntervalSeconds: Int? = null,
    val tokenExpireSeconds: Long? = null,
) {
    companion object {
        fun from(info: QueuePositionInfo): GetQueuePositionResult {
            return GetQueuePositionResult(
                status = info.status,
                position = info.position,
                totalWaiting = info.totalWaiting,
                estimatedWaitSeconds = info.estimatedWaitSeconds,
                pollingIntervalSeconds = info.pollingIntervalSeconds,
                tokenExpireSeconds = info.tokenExpireSeconds,
            )
        }
    }
}
