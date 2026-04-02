package com.loopers.domain.queue

interface EntryTokenRepository {

    companion object {
        const val BYPASS_TOKEN = "__BYPASS__"
    }

    fun issue(userId: Long, token: String, ttlSeconds: Long)

    fun get(userId: Long): String?

    fun consume(userId: Long)
}
