package com.loopers.application.service

import com.loopers.domain.dlq.DlqMessage
import com.loopers.domain.dlq.DlqStatus
import com.loopers.infrastructure.dlq.DlqMessageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DlqHandler")
class DlqHandlerTest {

    private lateinit var dlqMessageRepository: DlqMessageRepository
    private lateinit var dlqHandler: DlqHandler

    @BeforeEach
    fun setUp() {
        dlqMessageRepository = mockk(relaxed = true)
        dlqHandler = DlqHandler(dlqMessageRepository)
    }

    @DisplayName("실패한 메시지를 DLQ에 저장한다")
    @Test
    fun saveToDlq_saveMessage() {
        // Given
        val savedMessage = DlqMessage(
            id = 1L,
            originalTopic = "test-topic",
            messagePayload = "{\"test\": \"payload\"}",
            consumerGroup = "test-group",
            eventType = "TestEvent",
            errorMessage = "Test error",
            errorStackTrace = "stack trace",
            status = DlqStatus.PENDING,
        )

        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "test-topic",
            messagePayload = "{\"test\": \"payload\"}",
            consumerGroup = "test-group",
            eventType = "TestEvent",
            exception = Exception("Test error"),
        )

        // Then
        verify(exactly = 1) { dlqMessageRepository.save(any()) }
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.status).isEqualTo(DlqStatus.PENDING)
        assertThat(result.originalTopic).isEqualTo("test-topic")
        assertThat(result.consumerGroup).isEqualTo("test-group")
    }

    @DisplayName("저장된 메시지는 PENDING 상태이다")
    @Test
    fun saveToDlq_statusIsPending() {
        // Given
        val savedMessage = DlqMessage(
            id = 1L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            status = DlqStatus.PENDING,
        )
        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
        )

        // Then
        assertThat(result.status).isEqualTo(DlqStatus.PENDING)
    }

    @DisplayName("예외 정보를 저장한다")
    @Test
    fun saveToDlq_saveExceptionInfo() {
        // Given
        val exception = RuntimeException("Test error message")
        val savedMessage = DlqMessage(
            id = 1L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            errorMessage = exception.message,
            errorStackTrace = exception.stackTraceToString(),
            status = DlqStatus.PENDING,
        )

        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            exception = exception,
        )

        // Then
        assertThat(result.errorMessage).isEqualTo("Test error message")
        assertThat(result.errorStackTrace).isNotEmpty()
    }

    @DisplayName("eventType 없이 저장할 수 있다")
    @Test
    fun saveToDlq_withoutEventType() {
        // Given
        val savedMessage = DlqMessage(
            id = 1L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            eventType = null,
            status = DlqStatus.PENDING,
        )

        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            eventType = null,
        )

        // Then
        assertThat(result.eventType).isNull()
    }

    @DisplayName("예외 없이 저장할 수 있다")
    @Test
    fun saveToDlq_withoutException() {
        // Given
        val savedMessage = DlqMessage(
            id = 1L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            errorMessage = null,
            errorStackTrace = null,
            status = DlqStatus.PENDING,
        )

        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
            exception = null,
        )

        // Then
        assertThat(result.errorMessage).isNull()
        assertThat(result.errorStackTrace).isNull()
    }

    @DisplayName("저장 후 저장된 메시지를 반환한다")
    @Test
    fun saveToDlq_returnSavedMessage() {
        // Given
        val savedMessage = DlqMessage(
            id = 123L,
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
        )
        every { dlqMessageRepository.save(any()) } returns savedMessage

        // When
        val result = dlqHandler.saveToDlq(
            originalTopic = "topic",
            messagePayload = "payload",
            consumerGroup = "group",
        )

        // Then
        assertThat(result.id).isEqualTo(123L)
    }

    @DisplayName("여러 메시지를 순차적으로 저장할 수 있다")
    @Test
    fun saveToDlq_multipleMessages() {
        // Given
        every { dlqMessageRepository.save(any()) } returns mockk(relaxed = true)

        // When
        dlqHandler.saveToDlq("topic1", "payload1", "group1")
        dlqHandler.saveToDlq("topic2", "payload2", "group2")
        dlqHandler.saveToDlq("topic3", "payload3", "group3")

        // Then
        verify(exactly = 3) { dlqMessageRepository.save(any()) }
    }
}
