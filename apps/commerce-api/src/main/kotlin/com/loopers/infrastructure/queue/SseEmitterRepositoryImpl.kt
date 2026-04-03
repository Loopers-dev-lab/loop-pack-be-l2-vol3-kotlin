package com.loopers.infrastructure.queue

import com.loopers.domain.queue.QueueEmitterRepository
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class SseEmitterRepositoryImpl : QueueEmitterRepository {

    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    override fun add(userId: Long, emitter: SseEmitter) {
        emitters[userId] = emitter
    }

    override fun get(userId: Long): SseEmitter? {
        return emitters[userId]
    }

    override fun remove(userId: Long) {
        emitters.remove(userId)
    }

    override fun getAll(): Map<Long, SseEmitter> {
        return emitters.toMap()
    }
}
