package com.loopers.domain.orderqueue

interface OrderQueueRepository {
    fun enqueue(userId: Long): Long
    fun getPosition(userId: Long): Long?
    fun getTotalSize(): Long
    fun dequeueAndIssueTokens(count: Long, ttlSeconds: Long): Long
    fun hasToken(userId: Long): Boolean
    fun consumeToken(userId: Long): Boolean
    fun getTokenTtl(userId: Long): Long
}
