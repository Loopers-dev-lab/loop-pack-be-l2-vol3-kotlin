package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QueueAdmissionSchedulerTest {

    @Mock
    private lateinit var orderQueueService: OrderQueueService

    @Mock
    private lateinit var queueFacade: QueueFacade

    private lateinit var scheduler: QueueAdmissionScheduler

    @BeforeEach
    fun setUp() {
        scheduler = QueueAdmissionScheduler(orderQueueService, queueFacade, 18L)
    }

    @Nested
    @DisplayName("입장 허용 스케줄링 실행 시,")
    inner class AdmitScheduling {

        @Test
        @DisplayName("설정된 배치 크기로 admitUsers를 호출한다")
        fun callsAdmitUsersWithConfiguredBatchSize() {
            // arrange
            whenever(orderQueueService.admitUsers(18L)).thenReturn(listOf(1L, 2L, 3L, 4L, 5L))

            // act
            scheduler.admitUsers()

            // assert
            verify(orderQueueService).admitUsers(18L)
        }

        @Test
        @DisplayName("대기열이 비어있어도 예외 없이 정상 종료한다")
        fun completesNormally_whenQueueIsEmpty() {
            // arrange
            whenever(orderQueueService.admitUsers(18L)).thenReturn(emptyList())

            // act & assert — 예외 없이 정상 종료
            scheduler.admitUsers()

            // assert
            verify(orderQueueService).admitUsers(18L)
        }

        @Test
        @DisplayName("admitUsers 실행 후 broadcastPositions를 호출한다")
        fun callsBroadcastPositionsAfterAdmittingUsers() {
            // arrange
            val admittedUserIds = listOf(1L, 2L, 3L)
            whenever(orderQueueService.admitUsers(18L)).thenReturn(admittedUserIds)

            // act
            scheduler.admitUsers()

            // assert
            verify(queueFacade).broadcastPositions(admittedUserIds)
        }
    }
}
