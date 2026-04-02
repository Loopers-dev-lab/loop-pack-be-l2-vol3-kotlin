package com.loopers.domain.queue

interface QueueTokenRepository {
    fun issueToken(userId: Long, ttlSeconds: Long): String
    fun getToken(userId: Long): String?
    fun deleteToken(userId: Long): Boolean
    fun hasToken(userId: Long): Boolean
    fun countActiveTokens(): Long
    fun getActiveTokenCount(): Long
    fun setActiveTokenCount(count: Long)
    fun incrementActiveTokenCount(delta: Long): Long
    fun decrementActiveTokenCount(): Long
}
