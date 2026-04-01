package com.loopers.application.queue

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class QueueFallbackHandler {

    private val log = LoggerFactory.getLogger(javaClass)
    private val available = AtomicBoolean(true)

    fun isAvailable(): Boolean = available.get()

    fun markUnavailable(reason: String) {
        if (available.compareAndSet(true, false)) {
            log.warn("[Queue Fallback 진입] {} — 대기열/토큰 검증 우회 모드", sanitize(reason))
        }
    }

    companion object {
        internal fun sanitize(reason: String): String =
            reason
                .replace(Regex("password=[^\\s,;]+"), "password=***")
                .replace(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?"), "***:***")
    }

    fun markAvailable() {
        if (available.compareAndSet(false, true)) {
            log.info("[Queue Fallback 복구] Redis 연결 복구 — 정상 모드 전환")
        }
    }
}
