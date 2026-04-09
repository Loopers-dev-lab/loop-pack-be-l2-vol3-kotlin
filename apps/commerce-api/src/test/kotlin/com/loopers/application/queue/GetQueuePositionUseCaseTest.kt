package com.loopers.application.queue

import com.loopers.domain.queue.fixture.FakeEntryTokenRepository
import com.loopers.domain.queue.fixture.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetQueuePositionUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var getQueuePositionUseCase: GetQueuePositionUseCase

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        getQueuePositionUseCase = GetQueuePositionUseCase(waitingQueueRepository, entryTokenRepository)
    }

    @Test
    fun `대기열에 있는 유저는 WAITING 상태와 순번을 반환해야 한다`() {
        waitingQueueRepository.enqueue(USER_ID, 1.0)

        val result = getQueuePositionUseCase.getPosition(USER_ID)

        assertThat(result.status).isEqualTo(QueuePositionResult.PositionStatus.WAITING)
        assertThat(result.position).isEqualTo(1)
    }

    @Test
    fun `토큰이 발급된 유저는 AUTHORIZED 상태를 반환해야 한다`() {
        entryTokenRepository.issueToken(USER_ID, "my-token", 300)

        val result = getQueuePositionUseCase.getPosition(USER_ID)

        assertThat(result.status).isEqualTo(QueuePositionResult.PositionStatus.AUTHORIZED)
        assertThat(result.token).isEqualTo("my-token")
    }

    @Test
    fun `대기열에 없는 유저는 NOT_IN_QUEUE 상태를 반환해야 한다`() {
        val result = getQueuePositionUseCase.getPosition(USER_ID)

        assertThat(result.status).isEqualTo(QueuePositionResult.PositionStatus.NOT_IN_QUEUE)
    }

    companion object {
        private const val USER_ID = 1L
    }
}
