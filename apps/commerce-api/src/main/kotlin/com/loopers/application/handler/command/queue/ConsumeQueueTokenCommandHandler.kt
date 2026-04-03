package com.loopers.application.handler.command.queue

import com.loopers.application.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ConsumeQueueTokenCommandHandler(
    private val queueService: QueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(memberId: Long) {
        try {
            queueService.consumeToken(memberId)
        } catch (e: Exception) {
            log.error("[ConsumeQueueToken] 토큰 삭제 실패 — 토큰 TTL 만료까지 잔존 가능 (memberId={})", memberId, e)
        }
    }
}
