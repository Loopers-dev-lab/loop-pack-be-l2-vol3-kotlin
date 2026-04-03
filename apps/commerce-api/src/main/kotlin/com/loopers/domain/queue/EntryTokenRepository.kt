package com.loopers.domain.queue

interface EntryTokenRepository {

    fun issue(userId: Long, token: String, ttlSeconds: Long)

    fun get(userId: Long): String?

    fun consume(userId: Long)
}
