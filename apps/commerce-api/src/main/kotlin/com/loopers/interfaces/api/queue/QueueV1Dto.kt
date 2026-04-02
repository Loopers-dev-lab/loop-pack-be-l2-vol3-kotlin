package com.loopers.interfaces.api.queue

class QueueV1Dto {

    data class EnterResponse(
        val queueName: String,
        val position: Long,
        val estimatedWaitSeconds: Long,
    )

    data class PositionResponse(
        val queueName: String,
        val position: Long,
        val estimatedWaitSeconds: Long,
        val token: String? = null,
    )

    data class StatusResponse(
        val queueName: String,
        val totalWaiting: Long,
        val throughputPerSecond: Long,
    )
}
