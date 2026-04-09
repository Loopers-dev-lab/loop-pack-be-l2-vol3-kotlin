package com.loopers.application.service

import com.loopers.domain.eventhandled.EventHandledDto
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.interfaces.consumer.EventHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

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

        // 3. event_handled 기록 (멱등성 완료)
        eventHandledRepository.save(EventHandledDto(dedupeKey = dedupeKey))

        // 4. Redis 랭킹 점수 반영: 트랜잭션 커밋 후 호출하여 DB 롤백 시 Redis 이중 적재 방지
        writeRankingAfterCommit(event)
    }

    private fun writeRankingAfterCommit(event: Any) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    productRankingWriteService.write(event)
                }
            })
        } else {
            // 트랜잭션이 없는 컨텍스트(테스트 등)에서는 직접 호출
            productRankingWriteService.write(event)
        }
    }
}
