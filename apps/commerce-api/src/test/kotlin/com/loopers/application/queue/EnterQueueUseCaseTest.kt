package com.loopers.application.queue

import com.loopers.domain.queue.fixture.FakeEntryTokenRepository
import com.loopers.domain.queue.fixture.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnterQueueUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var enterQueueUseCase: EnterQueueUseCase

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        enterQueueUseCase = EnterQueueUseCase(waitingQueueRepository, entryTokenRepository)
    }

    @Test
    fun `대기열 진입 시 QUEUED 상태와 순번을 반환해야 한다`() {
        val result = enterQueueUseCase.enter(USER_ID)

        assertThat(result.status).isEqualTo(QueueEntryResult.QueueStatus.QUEUED)
        assertThat(result.position).isEqualTo(1)
        assertThat(result.estimatedWaitSeconds).isNotNull()
        assertThat(result.totalWaiting).isEqualTo(1)
    }

    @Test
    fun `이미 토큰이 발급된 유저는 ALREADY_AUTHORIZED를 반환해야 한다`() {
        entryTokenRepository.issueToken(USER_ID, "existing-token", 300)

        val result = enterQueueUseCase.enter(USER_ID)

        assertThat(result.status).isEqualTo(QueueEntryResult.QueueStatus.ALREADY_AUTHORIZED)
        assertThat(result.token).isEqualTo("existing-token")
    }

    @Test
    fun `중복 진입 시 기존 순번을 반환해야 한다`() {
        enterQueueUseCase.enter(USER_ID)
        val result = enterQueueUseCase.enter(USER_ID)

        assertThat(result.status).isEqualTo(QueueEntryResult.QueueStatus.QUEUED)
        assertThat(result.position).isEqualTo(1)
    }

    @Test
    fun `여러 유저가 진입하면 순번이 순서대로 부여되어야 한다`() {
        enterQueueUseCase.enter(1L)
        val result = enterQueueUseCase.enter(2L)

        assertThat(result.position).isEqualTo(2)
    }

    @Test
    fun `진입 결과에 전체 대기 인원이 포함되어야 한다`() {
        enterQueueUseCase.enter(1L)
        enterQueueUseCase.enter(2L)
        val result = enterQueueUseCase.enter(3L)

        assertThat(result.totalWaiting).isEqualTo(3)
    }

    companion object {
        private const val USER_ID = 1L
    }
}
