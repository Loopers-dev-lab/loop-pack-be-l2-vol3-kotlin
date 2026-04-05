package com.loopers.domain.queue

data class QueuedUser(
    val userId: Long,
    val score: Double,
)

interface QueueRepository {
    fun enter(queueName: String, userId: Long, score: Double): Boolean
    fun getRank(queueName: String, userId: Long): Long?
    fun size(queueName: String): Long
    fun remove(queueName: String, userId: Long)
    fun popMin(queueName: String, count: Long): List<QueuedUser>

    fun issueToken(queueName: String, userId: Long, token: String, ttlSeconds: Long)
    fun getToken(queueName: String, userId: Long): String?
    fun getAndConsume(queueName: String, userId: Long): String?
    fun deleteToken(queueName: String, userId: Long)
}
