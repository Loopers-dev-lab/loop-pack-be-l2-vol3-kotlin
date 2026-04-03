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
import java.io.IOException
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

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

        @Test
        @DisplayName("SseEmitter의 onCompletion/onTimeout/onError 시 저장소에서 자동 제거된다")
        fun removesEmitterFromRepository_whenCallbacksFired() {
            // arrange
            val userId = 1L
            val emitter = queueFacade.subscribe(userId)
            assertThat(queueEmitterRepository.get(userId)).isNotNull()

            // act - onCompletion 콜백 추출 및 실행 (서블릿 컨텍스트 없이 콜백을 직접 트리거)
            val field = ResponseBodyEmitter::class.java.getDeclaredField("completionCallback")
            field.isAccessible = true
            val callback = field.get(emitter) as Runnable
            callback.run()

            // assert
            assertThat(queueEmitterRepository.get(userId)).isNull()
        }
    }

    @Nested
    @DisplayName("broadcastBypass 호출할 때,")
    inner class BroadcastBypass {

        @Test
        @DisplayName("모든 SSE 구독자에게 bypass 이벤트를 전송하고 complete한다")
        fun sendsBypassEventAndCompletes_forAllSubscribers() {
            // arrange
            val emitter1 = mock<SseEmitter>()
            val emitter2 = mock<SseEmitter>()
            queueEmitterRepository.add(1L, emitter1)
            queueEmitterRepository.add(2L, emitter2)

            // act
            queueFacade.broadcastBypass()

            // assert
            val inOrder1 = inOrder(emitter1)
            inOrder1.verify(emitter1).send(any<SseEmitter.SseEventBuilder>())
            inOrder1.verify(emitter1).complete()

            val inOrder2 = inOrder(emitter2)
            inOrder2.verify(emitter2).send(any<SseEmitter.SseEventBuilder>())
            inOrder2.verify(emitter2).complete()
        }

        @Test
        @DisplayName("전송 실패한 SseEmitter는 저장소에서 제거된다")
        fun removesEmitterFromRepository_whenSendFails() {
            // arrange
            val emitter = mock<SseEmitter>()
            queueEmitterRepository.add(1L, emitter)
            whenever(emitter.send(any<SseEmitter.SseEventBuilder>())).thenThrow(IOException("connection closed"))

            // act
            queueFacade.broadcastBypass()

            // assert
            assertThat(queueEmitterRepository.get(1L)).isNull()
        }
    }

    @Nested
    @DisplayName("broadcastPositions 호출할 때,")
    inner class BroadcastPositions {

        @Test
        @DisplayName("토큰이 발급된 유저에게 admitted 이벤트를 전송하고 SseEmitter를 완료한다")
        fun sendsAdmittedEventAndCompletesEmitter_forAdmittedUsers() {
            // arrange
            val admittedUserId = 1L
            val emitter = mock<SseEmitter>()
            queueEmitterRepository.add(admittedUserId, emitter)

            // act
            queueFacade.broadcastPositions(listOf(admittedUserId))

            // assert
            val inOrder = inOrder(emitter)
            inOrder.verify(emitter).send(any<SseEmitter.SseEventBuilder>())
            inOrder.verify(emitter).complete()
        }

        @Test
        @DisplayName("대기 중인 유저에게 position 이벤트를 전송한다")
        fun sendsPositionEvent_forWaitingUsers() {
            // arrange
            val waitingUserId = 2L
            val emitter = mock<SseEmitter>()
            queueEmitterRepository.add(waitingUserId, emitter)

            whenever(orderQueueService.getWaitingPositions(listOf(waitingUserId))).thenReturn(
                mapOf(waitingUserId to QueuePosition(position = 5L, estimatedWaitSeconds = 5 / 175.0, totalSize = 100L)),
            )

            // act
            queueFacade.broadcastPositions(listOf(1L))

            // assert
            verify(emitter).send(any<SseEmitter.SseEventBuilder>())
            verify(emitter, never()).complete()
        }

        @Test
        @DisplayName("전송 실패한 SseEmitter는 저장소에서 제거된다")
        fun removesEmitterFromRepository_whenSendFails() {
            // arrange
            val waitingUserId = 2L
            val emitter = mock<SseEmitter>()
            queueEmitterRepository.add(waitingUserId, emitter)

            whenever(emitter.send(any<SseEmitter.SseEventBuilder>())).thenThrow(IOException("connection closed"))
            whenever(orderQueueService.getWaitingPositions(listOf(waitingUserId))).thenReturn(
                mapOf(waitingUserId to QueuePosition(position = 5L, estimatedWaitSeconds = 5 / 175.0, totalSize = 100L)),
            )

            // act
            queueFacade.broadcastPositions(listOf(1L))

            // assert
            assertThat(queueEmitterRepository.get(waitingUserId)).isNull()
        }
    }
}
