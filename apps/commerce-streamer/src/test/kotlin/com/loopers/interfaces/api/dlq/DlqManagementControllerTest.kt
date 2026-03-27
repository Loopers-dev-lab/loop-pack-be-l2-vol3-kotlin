package com.loopers.interfaces.api.dlq

import com.loopers.domain.dlq.DlqMessage
import com.loopers.domain.dlq.DlqStatus
import com.loopers.infrastructure.dlq.DlqMessageRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@DisplayName("DlqManagementController")
class DlqManagementControllerTest {

    private lateinit var dlqMessageRepository: DlqMessageRepository
    private lateinit var controller: DlqManagementController

    @BeforeEach
    fun setUp() {
        dlqMessageRepository = mockk()
        controller = DlqManagementController(dlqMessageRepository)
    }

    @DisplayName("DLQ 메시지 목록을 조회한다")
    @Test
    fun listDlqMessages_returnPageOfMessages() {
        // Given
        val messages = listOf(
            DlqMessage(
                id = 1L,
                originalTopic = "topic1",
                messagePayload = "payload1",
                consumerGroup = "group1",
                status = DlqStatus.PENDING,
            ),
            DlqMessage(
                id = 2L,
                originalTopic = "topic2",
                messagePayload = "payload2",
                consumerGroup = "group2",
                status = DlqStatus.PENDING,
            ),
        )
        val page = PageImpl(messages, PageRequest.of(0, 20), 2)

        every {
            dlqMessageRepository.findAll(any<PageRequest>())
        } returns page

        // When
        val result = controller.listDlqMessages(page = 0, size = 20, status = null)

        // Then
        assertThat(result.content).hasSize(2)
        assertThat(result.totalElements).isEqualTo(2)
    }

    @DisplayName("상태별로 DLQ 메시지를 필터링한다")
    @Test
    fun listDlqMessages_filterByStatus() {
        // Given
        val messages = listOf(
            DlqMessage(
                id = 1L,
                originalTopic = "topic",
                messagePayload = "payload",
                consumerGroup = "group",
                status = DlqStatus.PENDING,
            ),
        )
        val page = PageImpl(messages, PageRequest.of(0, 20), 1)

        every {
            dlqMessageRepository.findByStatus(DlqStatus.PENDING, any<PageRequest>())
        } returns page

        // When
        val result = controller.listDlqMessages(page = 0, size = 20, status = "PENDING")

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].status).isEqualTo(DlqStatus.PENDING)
    }

    @DisplayName("특정 DLQ 메시지를 조회한다")
    @Test
    fun getDlqMessage_returnMessage() {
        // Given
        val message = DlqMessage(
            id = 1L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            errorMessage = "Error message",
            errorStackTrace = "Stack trace",
            status = DlqStatus.PENDING,
        )

        every { dlqMessageRepository.findById(1L) } returns Optional.of(message)

        // When
        val result = controller.getDlqMessage(1L)

        // Then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.errorMessage).isEqualTo("Error message")
    }

    @DisplayName("존재하지 않는 메시지 조회 시 예외를 던진다")
    @Test
    fun getDlqMessage_throwExceptionWhenNotFound() {
        // Given
        every { dlqMessageRepository.findById(999L) } returns Optional.empty()

        // When & Then
        try {
            controller.getDlqMessage(999L)
            assert(false) { "Should throw IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("DLQ message not found")
        }
    }

    @DisplayName("특정 토픽의 DLQ 통계를 조회한다")
    @Test
    fun getDlqStats_returnStatistics() {
        // Given
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.PENDING, "metrics-events")
        } returns 2L
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.DEAD_LETTERED, "metrics-events")
        } returns 0L
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.RESOLVED, "metrics-events")
        } returns 1L

        // When
        val result = controller.getDlqStats("metrics-events")

        // Then
        assertThat(result["topic"]).isEqualTo("metrics-events")
        assertThat(result["total"]).isEqualTo(3L)
        assertThat(result["pending"]).isEqualTo(2L)
        assertThat(result["deadLettered"]).isEqualTo(0L)
        assertThat(result["resolved"]).isEqualTo(1L)
    }

    @DisplayName("DLQ 전체 통계를 조회한다")
    @Test
    fun getDlqSummary_returnTotalStatistics() {
        // Given
        every { dlqMessageRepository.countByStatus(DlqStatus.PENDING) } returns 5L
        every { dlqMessageRepository.countByStatus(DlqStatus.RESOLVED) } returns 3L
        every { dlqMessageRepository.countByStatus(DlqStatus.DEAD_LETTERED) } returns 1L

        // When
        val result = controller.getDlqSummary()

        // Then
        assertThat(result["pending"]).isEqualTo(5L)
        assertThat(result["resolved"]).isEqualTo(3L)
        assertThat(result["deadLettered"]).isEqualTo(1L)
    }

    @DisplayName("DLQ 요약에는 retrying 상태가 포함되지 않는다")
    @Test
    fun getDlqSummary_noRetryingStatus() {
        // Given
        every { dlqMessageRepository.countByStatus(DlqStatus.PENDING) } returns 5L
        every { dlqMessageRepository.countByStatus(DlqStatus.RESOLVED) } returns 3L
        every { dlqMessageRepository.countByStatus(DlqStatus.DEAD_LETTERED) } returns 1L

        // When
        val result = controller.getDlqSummary()

        // Then
        assertThat(result).doesNotContainKey("retrying")
    }

    @DisplayName("상태별로 메시지를 정확히 세개 집계한다")
    @Test
    fun getDlqStats_countEachStatusCorrectly() {
        // Given
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.PENDING, "topic")
        } returns 2L
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.DEAD_LETTERED, "topic")
        } returns 0L
        every {
            dlqMessageRepository.countByStatusAndOriginalTopic(DlqStatus.RESOLVED, "topic")
        } returns 1L

        // When
        val result = controller.getDlqStats("topic")

        // Then
        assertThat(result["pending"]).isEqualTo(2L)
        assertThat(result["deadLettered"]).isEqualTo(0L)
        assertThat(result["resolved"]).isEqualTo(1L)
    }

    @DisplayName("페이지네이션을 지원한다")
    @Test
    fun listDlqMessages_supportPagination() {
        // Given
        val messages = listOf(
            DlqMessage(1L, "topic", "payload", "group"),
        )
        val page = PageImpl(messages, PageRequest.of(1, 10), 50)

        every { dlqMessageRepository.findAll(any<PageRequest>()) } returns page

        // When
        val result = controller.listDlqMessages(page = 1, size = 10, status = null)

        // Then
        assertThat(result.number).isEqualTo(1)
        assertThat(result.size).isEqualTo(10)
        assertThat(result.totalElements).isEqualTo(50)
    }
}
