package com.loopers.application.queue

interface QueueTokenStore {

    fun issue(memberId: Long, token: String, ttlSeconds: Long): Boolean

    fun get(memberId: Long): String?

    fun delete(memberId: Long): Boolean

    fun activeCount(): Long
}
