package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import com.loopers.domain.queue.QueueEmitterRepository
import com.loopers.domain.queue.QueuePosition
import com.loopers.infrastructure.queue.SseEmitterRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QueueFacadeTest {

    @Mock
    private lateinit var orderQueueService: OrderQueueService

    private lateinit var queueEmitterRepository: QueueEmitterRepository

    private lateinit var queueFacade: QueueFacade

    @BeforeEach
    fun setUp() {
        queueEmitterRepository = SseEmitterRepositoryImpl()
        queueFacade = QueueFacade(orderQueueService, queueEmitterRepository)
    }

    @Nested
    @DisplayName("대기열 진입할 때,")
    inner class EnterQueue {

        @Test
        @DisplayName("신규 유저이면 대기열에 진입하고 순번 정보를 반환한다")
        fun returnsQueuePositionInfo_whenNewUser() {
            // arrange
            val userId = 1L
            whenever(orderQueueService.enterQueue(userId)).thenReturn(true)
            whenever(orderQueueService.getPosition(userId)).thenReturn(
                QueuePosition(position = 5L, estimatedWaitSeconds = 5 / 175.0, totalSize = 100L),
            )

            // act
            val result = queueFacade.enterQueue(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(5L) },
                { assertThat(result.totalSize).isEqualTo(100L) },
                { assertThat(result.pollingIntervalMs).isEqualTo(1000L) },
            )
        }

        @Test
        @DisplayName("이미 대기열에 있는 유저는 현재 순번을 반환한다")
        fun returnsCurrentPosition_whenAlreadyInQueue() {
            // arrange
            val userId = 1L
            whenever(orderQueueService.enterQueue(userId)).thenReturn(false)
            whenever(orderQueueService.getPosition(userId)).thenReturn(
                QueuePosition(position = 3L, estimatedWaitSeconds = 3 / 175.0, totalSize = 50L),
            )

            // act
            val result = queueFacade.enterQueue(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(3L) },
                { assertThat(result.totalSize).isEqualTo(50L) },
            )
        }
    }

    @Nested
    @DisplayName("순번 조회할 때,")
    inner class GetPosition {

        @Test
        @DisplayName("대기열에 있으면 순번과 polling 주기를 반환한다")
        fun returnsPositionWithPollingInterval_whenInQueue() {
            // arrange
            val userId = 1L
            whenever(orderQueueService.getPosition(userId)).thenReturn(
                QueuePosition(position = 30L, estimatedWaitSeconds = 30 / 175.0, totalSize = 200L),
            )

            // act
            val result = queueFacade.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(30L) },
                { assertThat(result.totalSize).isEqualTo(200L) },
                { assertThat(result.pollingIntervalMs).isEqualTo(2000L) },
                { assertThat(result.token).isNull() },
            )
        }

        @Test
        @DisplayName("토큰이 발급되었으면 position=0, pollingIntervalMs=0을 반환한다")
        fun returnsZeroPositionWithToken_whenTokenIssued() {
            // arrange
            val userId = 1L
            whenever(orderQueueService.getPosition(userId)).thenReturn(
                QueuePosition(position = 0L, estimatedWaitSeconds = 0.0, totalSize = 0L, token = "issued-token"),
            )

            // act
            val result = queueFacade.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isEqualTo("issued-token") },
                { assertThat(result.pollingIntervalMs).isEqualTo(0L) },
            )
        }

        @ParameterizedTest(name = "position={0}이면 pollingIntervalMs={1}을 반환한다")
        @CsvSource("1,1000", "10,1000", "11,2000", "50,2000", "51,3000", "200,3000", "201,5000", "500,5000")
        @DisplayName("동적 Polling 주기 계산")
        fun calculatesPollingInterval(position: Long, expectedMs: Long) {
            // arrange
            val userId = 1L
            whenever(orderQueueService.getPosition(userId)).thenReturn(
                QueuePosition(position = position, estimatedWaitSeconds = position / 175.0, totalSize = 1000L),
            )

            // act
            val result = queueFacade.getPosition(userId)

            // assert
            assertThat(result.pollingIntervalMs).isEqualTo(expectedMs)
        }
    }

    @Nested
    @DisplayName("SSE 구독할 때,")
    inner class Subscribe {

        @Test
        @DisplayName("SseEmitter를 생성하고 저장소에 등록하여 반환한다")
        fun returnsSseEmitter_andRegistersInRepository() {
            // arrange
            val userId = 1L

            // act
            val emitter = queueFacade.subscribe(userId)

            // assert
            assertAll(
                { assertThat(emitter).isNotNull },
                { assertThat(queueEmitterRepository.get(userId)).isSameAs(emitter) },
            )
        }
    }
}
