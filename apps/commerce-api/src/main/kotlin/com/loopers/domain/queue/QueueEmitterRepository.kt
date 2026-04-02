package com.loopers.domain.queue

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

interface QueueEmitterRepository {
    fun add(userId: Long, emitter: SseEmitter)
    fun get(userId: Long): SseEmitter?
    fun remove(userId: Long)
    fun getAll(): Map<Long, SseEmitter>
}
