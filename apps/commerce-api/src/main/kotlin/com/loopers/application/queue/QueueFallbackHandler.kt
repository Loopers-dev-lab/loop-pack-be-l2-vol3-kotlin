package com.loopers.application.queue

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class QueueFallbackHandler {

    private val log = LoggerFactory.getLogger(javaClass)
    private val available = AtomicBoolean(true)

    fun isAvailable(): Boolean = available.get()

    fun markUnavailable() {
        if (available.compareAndSet(true, false)) {
            log.warn("[Queue Fallback 진입] Fail-Fast 모드 진입")
        }
    }

    fun markAvailable() {
        if (available.compareAndSet(false, true)) {
            log.info("[Queue Fallback 복구] Redis 연결 복구 — 정상 모드 전환")
        }
    }
}
