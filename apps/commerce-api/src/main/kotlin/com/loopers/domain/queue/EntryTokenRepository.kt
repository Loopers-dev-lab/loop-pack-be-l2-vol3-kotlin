package com.loopers.domain.queue

interface EntryTokenRepository {
    fun issueToken(userId: Long, token: String, ttlSeconds: Long)
    fun findToken(userId: Long): String?
    fun deleteToken(userId: Long)
    fun hasToken(userId: Long): Boolean
}
