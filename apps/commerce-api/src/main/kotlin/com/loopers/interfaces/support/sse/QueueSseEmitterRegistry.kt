package com.loopers.interfaces.support.sse

import com.loopers.application.queue.QueueProperties
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class QueueSseEmitterRegistry(
    private val queueProperties: QueueProperties,
) {

    private val emitters = ConcurrentHashMap<Long, SseEmitter>()

    fun register(userId: Long): SseEmitter {
        emitters.remove(userId)?.complete()

        val emitter = SseEmitter(queueProperties.sseTimeoutMs)
        emitters[userId] = emitter

        // ConcurrentHashMap.remove(key, value)로 최신 emitter만 보호
        emitter.onCompletion { emitters.remove(userId, emitter) }
        emitter.onTimeout { emitters.remove(userId, emitter) }
        emitter.onError { emitters.remove(userId, emitter) }

        return emitter
    }

    fun sendEvent(userId: Long, eventName: String, data: Any) {
        val emitter = emitters[userId] ?: return
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .data(data),
            )
        } catch (e: Exception) {
            emitters.remove(userId, emitter)
        }
    }

    fun complete(userId: Long) {
        emitters.remove(userId)?.complete()
    }

    fun connectedUserIds(): Set<Long> = emitters.keys.toSet()
}
