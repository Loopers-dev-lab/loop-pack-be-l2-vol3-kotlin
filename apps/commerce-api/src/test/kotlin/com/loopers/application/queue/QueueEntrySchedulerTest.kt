package com.loopers.application.queue

import com.loopers.domain.queue.QueueThroughput
import com.loopers.domain.queue.fixture.FakeEntryTokenRepository
import com.loopers.domain.queue.fixture.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QueueEntrySchedulerTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var scheduler: QueueEntryScheduler

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        scheduler = QueueEntryScheduler(waitingQueueRepository, entryTokenRepository)
    }

    @Test
    fun `스케줄러 실행 시 대기열에서 유저를 꺼내 토큰을 발급해야 한다`() {
        waitingQueueRepository.enqueue(1L, 1.0)
        waitingQueueRepository.enqueue(2L, 2.0)
        waitingQueueRepository.enqueue(3L, 3.0)

        scheduler.processQueue()

        assertThat(entryTokenRepository.hasToken(1L)).isTrue()
        assertThat(entryTokenRepository.hasToken(2L)).isTrue()
        assertThat(entryTokenRepository.hasToken(3L)).isTrue()
        assertThat(waitingQueueRepository.getQueueSize()).isZero()
    }

    @Test
    fun `대기열이 비어있으면 아무 작업도 하지 않아야 한다`() {
        scheduler.processQueue()

        assertThat(waitingQueueRepository.getQueueSize()).isZero()
    }

    @Test
    fun `배치 크기보다 많은 유저가 있으면 배치 크기만큼만 처리해야 한다`() {
        // BATCH_SIZE가 재산정으로 바뀌어도 "초과 인원" 시나리오가 유지되도록 상수에서 파생한다.
        val overflow = 11L
        val userCount = QueueThroughput.BATCH_SIZE + overflow
        for (i in 1..userCount) {
            waitingQueueRepository.enqueue(i, i.toDouble())
        }

        scheduler.processQueue()

        assertThat(waitingQueueRepository.getQueueSize()).isEqualTo(overflow)
    }
}
