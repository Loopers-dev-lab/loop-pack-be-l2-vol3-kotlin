package com.loopers.application.service

import com.loopers.domain.eventhandled.EventHandledDto
import com.loopers.domain.eventhandled.EventHandledRepository
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.interfaces.consumer.EventHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LikeCountService 단위 테스트")
class LikeCountServiceTest {

    private lateinit var eventHandledRepository: EventHandledRepository
    private lateinit var productRankingWriteService: ProductRankingWriteService
    private lateinit var handlers: Map<String, EventHandler>
    private lateinit var service: LikeCountService

    @BeforeEach
    fun setUp() {
        eventHandledRepository = mockk()
        productRankingWriteService = mockk(relaxed = true)
        handlers = mapOf(
            "LikeCountEvent" to mockk<EventHandler>(relaxed = true),
        )
        service = LikeCountService(eventHandledRepository, productRankingWriteService, handlers)
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 무시한다")
    fun shouldIgnoreDuplicateEvents() {
        val event = LikeCountEvent(productId = 1L, type = LikeCountEventType.INCREMENT, userId = 1L, dedupeKey = "key-1")
        every { eventHandledRepository.existsByDedupeKey("key-1") } returns true

        service.processLikeCountEvent(event)

        verify(exactly = 0) { eventHandledRepository.save(any()) }
        verify(exactly = 0) { productRankingWriteService.write(any()) }
    }

    @Test
    @DisplayName("Redis 랭킹 쓰기는 event_handled 저장 이후에 호출된다")
    fun rankingWriteIsCalledAfterDbSave() {
        val event = LikeCountEvent(productId = 1L, type = LikeCountEventType.INCREMENT, userId = 1L, dedupeKey = "key-2")
        every { eventHandledRepository.existsByDedupeKey("key-2") } returns false
        every { eventHandledRepository.save(any()) } returns EventHandledDto(dedupeKey = "key-2")

        service.processLikeCountEvent(event)

        verifyOrder {
            eventHandledRepository.save(any())
            productRankingWriteService.write(event)
        }
    }
}
