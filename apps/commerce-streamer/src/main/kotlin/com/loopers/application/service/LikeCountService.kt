package com.loopers.application.service

import com.loopers.domain.eventhandled.EventHandledDto
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LikeCountService(
    private val eventHandledRepository: EventHandledRepository,
    private val productRankingWriteService: ProductRankingWriteService,
    private val handlers: Map<String, EventHandler>,
) {
    fun processLikeCountEvent(event: LikeCountEvent) {
        val dedupeKey = event.dedupeKey

        // 1. 멱등성 검증 - 이미 처리된 이벤트면 return
        if (eventHandledRepository.existsByDedupeKey(dedupeKey)) {
            return
        }

        // 2. 이벤트 핸들러 호출
        val handler = handlers["LikeCountEvent"]
        handler?.handle(event)

        // 3. Redis 랭킹 점수 반영 (handled-marker 저장 전)
        productRankingWriteService.write(event)

        // 4. event_handled 기록 (멱등성 완료)
        eventHandledRepository.save(EventHandledDto(dedupeKey = dedupeKey))
    }
}
