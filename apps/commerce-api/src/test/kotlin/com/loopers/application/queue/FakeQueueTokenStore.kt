package com.loopers.application.queue

class FakeQueueTokenStore : QueueTokenStore {

    private val tokens = mutableMapOf<Long, String>()

    override fun issue(memberId: Long, token: String, ttlSeconds: Long): Boolean {
        if (tokens.containsKey(memberId)) return false
        tokens[memberId] = token
        return true
    }

    override fun get(memberId: Long): String? = tokens[memberId]

    override fun delete(memberId: Long): Boolean = tokens.remove(memberId) != null

    override fun activeCount(): Long = tokens.size.toLong()
}
