package com.loopers.domain.queue

interface OrderQueueStore {
    fun enqueue(userId: Long, maxQueueSize: Long): Long
    fun getPosition(userId: Long): Long?
    fun getQueueSize(): Long
    fun hasToken(userId: Long): Boolean
    fun consumeToken(userId: Long): Boolean
    fun deleteToken(userId: Long)
    fun issueTokens(batchSize: Long, ttlSeconds: Long): Long
}
