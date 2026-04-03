package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import com.loopers.domain.queue.QueueHealthChecker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QueueAdmissionSchedulerTest {

    @Mock
    private lateinit var orderQueueService: OrderQueueService

    @Mock
    private lateinit var queueFacade: QueueFacade

    @Mock
    private lateinit var queueHealthChecker: QueueHealthChecker

    private lateinit var scheduler: QueueAdmissionScheduler

    @BeforeEach
    fun setUp() {
        scheduler = QueueAdmissionScheduler(
            orderQueueService = orderQueueService,
            queueFacade = queueFacade,
            queueHealthChecker = queueHealthChecker,
            batchSize = 9L,
            fixedRate = 50L,
            jitterRange = 20L,
        )
    }

    @Nested
    @DisplayName("입장 허용 스케줄링 실행 시,")
    inner class AdmitScheduling {

        @Test
        @DisplayName("설정된 배치 크기로 admitUsers를 호출한다")
        fun callsAdmitUsersWithConfiguredBatchSize() {
            // arrange
            whenever(orderQueueService.admitUsers(9L)).thenReturn(listOf(1L, 2L, 3L))

            // act
            scheduler.admitUsers()

            // assert
            verify(orderQueueService).admitUsers(9L)
        }

        @Test
        @DisplayName("대기열이 비어있어도 예외 없이 정상 종료한다")
        fun completesNormally_whenQueueIsEmpty() {
            // arrange
            whenever(orderQueueService.admitUsers(9L)).thenReturn(emptyList())

            // act & assert — 예외 없이 정상 종료
            scheduler.admitUsers()

            // assert
            verify(orderQueueService).admitUsers(9L)
        }

        @Test
        @DisplayName("admitUsers 실행 후 broadcastPositions를 호출한다")
        fun callsBroadcastPositionsAfterAdmittingUsers() {
            // arrange
            val admittedUserIds = listOf(1L, 2L, 3L)
            whenever(orderQueueService.admitUsers(9L)).thenReturn(admittedUserIds)

            // act
            scheduler.admitUsers()

            // assert
            verify(queueFacade).broadcastPositions(admittedUserIds)
        }
    }

    @Nested
    @DisplayName("bypass 감지 시,")
    inner class BypassDetection {

        @Test
        @DisplayName("QueueHealthChecker가 bypass 상태이면 broadcastBypass를 호출하고 admitUsers는 호출하지 않는다")
        fun callsBroadcastBypass_whenBypassed() {
            // arrange
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act
            scheduler.admitUsers()

            // assert
            verify(queueFacade).broadcastBypass()
            verify(orderQueueService, never()).admitUsers(9L)
        }

        @Test
        @DisplayName("bypass 상태가 지속되면 broadcastBypass는 최초 1회만 호출한다")
        fun callsBroadcastBypassOnlyOnce_whenBypassPersists() {
            // arrange
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act
            scheduler.admitUsers()
            scheduler.admitUsers()
            scheduler.admitUsers()

            // assert
            verify(queueFacade, times(1)).broadcastBypass()
        }

        @Test
        @DisplayName("bypass 해제 후 다시 bypass되면 broadcastBypass를 다시 호출한다")
        fun callsBroadcastBypassAgain_whenBypassReoccurs() {
            // arrange & act — bypass → 정상 → bypass
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)
            scheduler.admitUsers()

            whenever(queueHealthChecker.isBypassed()).thenReturn(false)
            whenever(orderQueueService.admitUsers(9L)).thenReturn(emptyList())
            scheduler.admitUsers()

            whenever(queueHealthChecker.isBypassed()).thenReturn(true)
            scheduler.admitUsers()

            // assert
            verify(queueFacade, times(2)).broadcastBypass()
        }

        @Test
        @DisplayName("QueueHealthChecker가 정상 상태이면 기존 입장 허용 로직을 수행한다")
        fun performsNormalAdmission_whenNotBypassed() {
            // arrange
            whenever(queueHealthChecker.isBypassed()).thenReturn(false)
            whenever(orderQueueService.admitUsers(9L)).thenReturn(listOf(1L))

            // act
            scheduler.admitUsers()

            // assert
            verify(orderQueueService).admitUsers(9L)
            verify(queueFacade).broadcastPositions(listOf(1L))
            verify(queueFacade, never()).broadcastBypass()
        }
    }

    @Nested
    @DisplayName("Jitter delay 계산할 때,")
    inner class CalculateNextDelay {

        @Test
        @DisplayName("fixedRate=50, jitterRange=20이면 [30, 70] 범위의 값을 반환하고 변동이 있다")
        fun returnsVaryingDelaysWithinJitterRange() {
            // act
            val delays = (1..100).map { scheduler.calculateNextDelay() }

            // assert — 모든 값이 [30, 70] 범위 내
            delays.forEach { assertThat(it).isBetween(30L, 70L) }
            // assert — 항상 같은 값이 아니라 변동이 있어야 한다
            assertThat(delays.toSet()).hasSizeGreaterThan(1)
        }

        @Test
        @DisplayName("jitterRange=0이면 fixedRate를 그대로 반환한다")
        fun returnsExactFixedRate_whenJitterRangeIsZero() {
            // arrange
            val noJitterScheduler = QueueAdmissionScheduler(
                orderQueueService = orderQueueService,
                queueFacade = queueFacade,
                queueHealthChecker = queueHealthChecker,
                batchSize = 9L,
                fixedRate = 50L,
                jitterRange = 0L,
            )

            // act
            val delays = (1..100).map { noJitterScheduler.calculateNextDelay() }

            // assert — 모든 값이 정확히 fixedRate
            delays.forEach { assertThat(it).isEqualTo(50L) }
        }
    }

    @Nested
    @DisplayName("스케줄러 생명주기 관리할 때,")
    inner class Lifecycle {

        @Test
        @DisplayName("start() 호출 후 isRunning이 true를 반환한다")
        fun isRunningReturnsTrue_afterStart() {
            // act
            scheduler.start()

            // assert
            try {
                assertThat(scheduler.isRunning).isTrue()
            } finally {
                scheduler.stop()
            }
        }

        @Test
        @DisplayName("stop() 호출 후 isRunning이 false를 반환한다")
        fun isRunningReturnsFalse_afterStop() {
            // arrange
            scheduler.start()

            // act
            scheduler.stop()

            // assert
            assertThat(scheduler.isRunning).isFalse()
        }

        @Test
        @DisplayName("start() 호출 후 admitUsers가 Jitter 간격으로 주기 실행된다")
        fun executesAdmitUsersPeriodically_afterStart() {
            // arrange
            whenever(orderQueueService.admitUsers(9L)).thenReturn(emptyList())

            // act
            scheduler.start()
            Thread.sleep(200)
            scheduler.stop()

            // assert
            verify(orderQueueService, atLeastOnce()).admitUsers(9L)
        }
    }
}
