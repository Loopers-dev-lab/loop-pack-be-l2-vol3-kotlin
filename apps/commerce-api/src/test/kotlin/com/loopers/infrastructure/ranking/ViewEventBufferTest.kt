package com.loopers.infrastructure.ranking

import com.loopers.domain.event.EventTopics
import com.loopers.domain.event.OutboxEventService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ViewEventBufferTest {
    private lateinit var outboxEventService: OutboxEventService
    private lateinit var viewEventBuffer: ViewEventBuffer

    @BeforeEach
    fun setUp() {
        outboxEventService = mock()
        viewEventBuffer = ViewEventBuffer(outboxEventService)
    }

    @DisplayName("조회 이벤트를 기록할 때, ")
    @Nested
    inner class Record {
        @DisplayName("같은 상품의 조회수가 누적된다.")
        @Test
        fun accumulatesViewCounts() {
            // arrange
            val productId = 1L

            // act
            viewEventBuffer.record(productId)
            viewEventBuffer.record(productId)
            viewEventBuffer.record(productId)
            viewEventBuffer.flush()

            // assert
            val eventCaptor = argumentCaptor<Any>()
            verify(outboxEventService).saveOutboxEvent(
                aggregateType = eq("Ranking"),
                aggregateId = eq("view-batch"),
                eventType = eq("ProductViewedBatch"),
                topic = eq(EventTopics.CATALOG_EVENTS),
                event = eventCaptor.capture(),
            )
            val payload = eventCaptor.firstValue as ViewEventBuffer.ViewBatchPayload
            assertThat(payload.views).hasSize(1)
            assertThat(payload.views[0].productId).isEqualTo(productId)
            assertThat(payload.views[0].count).isEqualTo(3)
        }

        @DisplayName("여러 상품의 조회수가 각각 누적된다.")
        @Test
        fun accumulatesMultipleProducts() {
            // arrange & act
            viewEventBuffer.record(1L)
            viewEventBuffer.record(2L)
            viewEventBuffer.record(1L)
            viewEventBuffer.flush()

            // assert
            val eventCaptor = argumentCaptor<Any>()
            verify(outboxEventService).saveOutboxEvent(
                aggregateType = eq("Ranking"),
                aggregateId = eq("view-batch"),
                eventType = eq("ProductViewedBatch"),
                topic = eq(EventTopics.CATALOG_EVENTS),
                event = eventCaptor.capture(),
            )
            val payload = eventCaptor.firstValue as ViewEventBuffer.ViewBatchPayload
            assertThat(payload.views).hasSize(2)
            val viewMap = payload.views.associate { it.productId to it.count }
            assertThat(viewMap[1L]).isEqualTo(2)
            assertThat(viewMap[2L]).isEqualTo(1)
        }
    }

    @DisplayName("flush 할 때, ")
    @Nested
    inner class Flush {
        @DisplayName("버퍼가 비어있으면 Outbox에 저장하지 않는다.")
        @Test
        fun doesNotSave_whenBufferEmpty() {
            // act
            viewEventBuffer.flush()

            // assert
            verify(outboxEventService, never()).saveOutboxEvent(
                aggregateType = any(),
                aggregateId = any(),
                eventType = any(),
                topic = any(),
                event = any(),
            )
        }

        @DisplayName("flush 후 버퍼가 비워진다.")
        @Test
        fun clearsBufferAfterFlush() {
            // arrange
            viewEventBuffer.record(1L)
            viewEventBuffer.flush()

            // act - 두 번째 flush
            viewEventBuffer.flush()

            // assert - Outbox 저장이 한 번만 호출됨
            verify(outboxEventService).saveOutboxEvent(
                aggregateType = any(),
                aggregateId = any(),
                eventType = any(),
                topic = any(),
                event = any(),
            )
        }
    }

    @DisplayName("동시성 테스트: 여러 스레드에서 동시에 record 호출 시 정확한 합산")
    @Test
    fun concurrentRecordAccumulation() {
        // arrange
        val threadCount = 100
        val recordsPerThread = 1000
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        // act
        repeat(threadCount) {
            executor.submit {
                try {
                    repeat(recordsPerThread) {
                        viewEventBuffer.record(1L)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        viewEventBuffer.flush()

        // assert
        val eventCaptor = argumentCaptor<Any>()
        verify(outboxEventService).saveOutboxEvent(
            aggregateType = any(),
            aggregateId = any(),
            eventType = any(),
            topic = any(),
            event = eventCaptor.capture(),
        )
        val payload = eventCaptor.firstValue as ViewEventBuffer.ViewBatchPayload
        assertThat(payload.views).hasSize(1)
        assertThat(payload.views[0].count).isEqualTo(threadCount.toLong() * recordsPerThread)

        executor.shutdown()
    }
}
