package com.loopers.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RetryableRecordProcessor(
    private val dlqPublisher: DlqPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_BACKOFF_MS = 500L
    }

    fun processWithRetry(
        record: ConsumerRecord<String, String>,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        backoffMs: Long = DEFAULT_BACKOFF_MS,
        processor: (ConsumerRecord<String, String>) -> Unit,
    ) {
        var lastException: Exception? = null

        repeat(maxAttempts) { attempt ->
            try {
                processor(record)
                return
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    val delay = backoffMs * (attempt + 1)
                    log.warn(
                        "레코드 처리 실패, 재시도 예정 [{}/{}] [topic={}, offset={}, delay={}ms, error={}]",
                        attempt + 1,
                        maxAttempts,
                        record.topic(),
                        record.offset(),
                        delay,
                        e.message,
                    )
                    Thread.sleep(delay)
                }
            }
        }

        log.error(
            "레코드 처리 재시도 소진, DLQ 전송 [topic={}, offset={}, attempts={}]",
            record.topic(),
            record.offset(),
            maxAttempts,
        )
        dlqPublisher.publish(record, lastException!!)
    }
}
