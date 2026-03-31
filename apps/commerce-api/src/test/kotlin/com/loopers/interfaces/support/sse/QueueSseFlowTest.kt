package com.loopers.interfaces.support.sse

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueProperties
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.interfaces.api.queue.QueueV1Controller
import com.loopers.interfaces.support.scheduler.QueueScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SSE 대기열 이벤트 스트림 흐름")
class QueueSseFlowTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var registry: QueueSseEmitterRegistry
    private lateinit var controller: QueueV1Controller
    private lateinit var scheduler: QueueScheduler

    private val properties = QueueProperties(
        maxCapacity = 50_000,
        batchSize = 3,
        tokenTtlSeconds = 300,
        throughputTps = 175,
        schedulerDelayMs = 100,
        jitterMaxMs = 0,
        sseTimeoutMs = 60_000,
    )

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        registry = QueueSseEmitterRegistry(properties)

        val enterQueueUseCase = EnterQueueUseCase(waitingQueueRepository, entryTokenRepository, properties)
        val getQueuePositionUseCase = GetQueuePositionUseCase(waitingQueueRepository, entryTokenRepository, properties)
        val issueEntryTokensUseCase = IssueEntryTokensUseCase(waitingQueueRepository, entryTokenRepository, properties)

        controller = QueueV1Controller(enterQueueUseCase, getQueuePositionUseCase, registry)
        scheduler = QueueScheduler(issueEntryTokensUseCase, getQueuePositionUseCase, registry)
    }

    @Nested
    @DisplayName("SSE 연결 시")
    inner class StreamQueueEvents {

        @Test
        @DisplayName("SseEmitter를 생성하고 registry에 등록한다")
        fun streamsEvents_registersEmitter() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, 50_000)

            // act
            val emitter = controller.streamQueueEvents(1L)

            // assert
            assertThat(emitter).isNotNull()
            assertThat(registry.connectedUserIds()).contains(1L)
        }

        @Test
        @DisplayName("대기열에 없는 유저가 연결 시 emitter가 에러로 완료된다")
        fun streamsEvents_userNotInQueue_completesWithError() {
            // act
            val emitter = controller.streamQueueEvents(999L)

            // assert — emitter 반환은 되지만 에러로 완료됨
            assertThat(emitter).isNotNull()
        }
    }

    @Nested
    @DisplayName("스케줄러 실행 후 SSE push 시")
    inner class SchedulerSsePush {

        @Test
        @DisplayName("토큰 발급 후 해당 유저의 SSE 연결이 종료된다")
        fun scheduler_tokenIssued_completesEmitter() {
            // arrange — 유저 3명 진입 + SSE 연결
            waitingQueueRepository.enter(1L, 1000.0, 50_000)
            waitingQueueRepository.enter(2L, 2000.0, 50_000)
            waitingQueueRepository.enter(3L, 3000.0, 50_000)
            controller.streamQueueEvents(1L)
            controller.streamQueueEvents(2L)
            controller.streamQueueEvents(3L)
            assertThat(registry.connectedUserIds()).hasSize(3)

            // act — 스케줄러 실행 (batchSize=3이므로 전원 토큰 발급)
            scheduler.issueTokens()

            // assert — 토큰 발급된 유저의 SSE 연결이 모두 종료됨
            assertThat(registry.connectedUserIds()).isEmpty()
        }

        @Test
        @DisplayName("토큰 미발급 유저는 SSE 연결이 유지된다")
        fun scheduler_remainingUsers_keepConnection() {
            // arrange — 유저 5명 진입, 3명만 SSE 연결
            for (i in 1L..5L) {
                waitingQueueRepository.enter(i, i * 1000.0, 50_000)
            }
            controller.streamQueueEvents(1L)
            controller.streamQueueEvents(4L)
            controller.streamQueueEvents(5L)

            // act — 스케줄러 실행 (batchSize=3이므로 1,2,3 발급)
            scheduler.issueTokens()

            // assert — userId 1은 발급 후 종료, 4/5는 유지
            assertThat(registry.connectedUserIds()).containsExactlyInAnyOrder(4L, 5L)
        }

        @Test
        @DisplayName("대기열이 비어있으면 SSE push를 하지 않는다")
        fun scheduler_emptyQueue_noPush() {
            // arrange
            registry.register(1L)

            // act
            scheduler.issueTokens()

            // assert — emitter 상태 변화 없음
            assertThat(registry.connectedUserIds()).contains(1L)
        }
    }
}
