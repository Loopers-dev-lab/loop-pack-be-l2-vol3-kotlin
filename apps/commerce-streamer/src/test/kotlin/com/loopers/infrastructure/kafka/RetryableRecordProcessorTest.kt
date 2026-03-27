package com.loopers.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RetryableRecordProcessorTest {

    private val dlqPublisher: DlqPublisher = mock()
    private val processor = RetryableRecordProcessor(dlqPublisher)

    private fun createRecord(topic: String = "test-topic", value: String = "{}"): ConsumerRecord<String, String> {
        return ConsumerRecord(topic, 0, 0L, "key", value)
    }

    @DisplayName("재시도 동작")
    @Nested
    inner class Retry {

        @DisplayName("처리 성공 시 재시도 없이 완료되고 DLQ에 전송하지 않는다")
        @Test
        fun successOnFirstAttempt() {
            // arrange
            val record = createRecord()
            var processCount = 0

            // act
            processor.processWithRetry(record, maxAttempts = 3, backoffMs = 10) {
                processCount++
            }

            // assert
            assertAll(
                { assertThat(processCount).`as`("처리는 1번만 호출되어야 한다").isEqualTo(1) },
                { verify(dlqPublisher, never()).publish(any(), any()) },
            )
        }

        @DisplayName("첫 시도 실패 후 재시도에서 성공하면 DLQ에 전송하지 않는다")
        @Test
        fun successOnRetry() {
            // arrange
            val record = createRecord()
            var processCount = 0

            // act
            processor.processWithRetry(record, maxAttempts = 3, backoffMs = 10) {
                processCount++
                if (processCount == 1) throw RuntimeException("일시적 오류")
            }

            // assert
            assertAll(
                { assertThat(processCount).`as`("처리는 2번 호출되어야 한다 (1번 실패 + 1번 성공)").isEqualTo(2) },
                { verify(dlqPublisher, never()).publish(any(), any()) },
            )
        }

        @DisplayName("모든 재시도가 소진되면 DLQ로 전송한다")
        @Test
        fun exhaustedRetriesSendToDlq() {
            // arrange
            val record = createRecord()
            val expectedException = RuntimeException("영구적 오류")
            var processCount = 0

            // act
            processor.processWithRetry(record, maxAttempts = 3, backoffMs = 10) {
                processCount++
                throw expectedException
            }

            // assert
            assertAll(
                { assertThat(processCount).`as`("maxAttempts만큼 시도해야 한다").isEqualTo(3) },
                { verify(dlqPublisher).publish(record, expectedException) },
            )
        }

        @DisplayName("maxAttempts=1이면 재시도 없이 즉시 DLQ로 전송한다")
        @Test
        fun noRetryWithMaxAttemptsOne() {
            // arrange
            val record = createRecord()
            val expectedException = RuntimeException("즉시 실패")
            var processCount = 0

            // act
            processor.processWithRetry(record, maxAttempts = 1, backoffMs = 10) {
                processCount++
                throw expectedException
            }

            // assert
            assertAll(
                { assertThat(processCount).`as`("1번만 시도해야 한다").isEqualTo(1) },
                { verify(dlqPublisher).publish(record, expectedException) },
            )
        }

        @DisplayName("DLQ 전송이 실패해도 예외를 전파하지 않고 배치 처리를 계속한다")
        @Test
        fun dlqFailureDoesNotPropagate() {
            // arrange
            val record = createRecord()
            doThrow(RuntimeException("Kafka broker unavailable"))
                .whenever(dlqPublisher).publish(any(), any())

            // act — 예외가 전파되지 않아야 한다
            processor.processWithRetry(record, maxAttempts = 1, backoffMs = 10) {
                throw RuntimeException("처리 실패")
            }

            // assert — DLQ 전송이 시도되었으나 실패해도 정상 종료
            verify(dlqPublisher).publish(any(), any())
        }
    }
}
