package com.loopers.application.queue

interface OrderEntryQueueStrategy {
    val type: QueueStrategyType

    fun enter(memberId: Long): QueueInfo.Status

    fun getStatus(memberId: Long): QueueInfo.Status

    fun admit(batchSize: Int): Int

    fun validateToken(memberId: Long, token: String)

    fun complete(memberId: Long, token: String)
}
