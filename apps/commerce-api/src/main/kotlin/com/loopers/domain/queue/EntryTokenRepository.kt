package com.loopers.domain.queue

interface EntryTokenRepository {
    fun issue(userId: Long): EntryToken
    fun validate(userId: Long, token: String): Boolean
    fun validateAndConsume(userId: Long, token: String): Boolean
    fun exists(userId: Long): Boolean
    fun findByUserId(userId: Long): EntryToken?
}
