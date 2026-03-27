package com.loopers.application.service

import com.loopers.domain.dlq.DlqMessage
import com.loopers.domain.dlq.DlqStatus
import com.loopers.infrastructure.dlq.DlqMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DlqHandler(
    private val dlqMessageRepository: DlqMessageRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 실패한 메시지를 DLQ 테이블에 저장
     * (DLQ 발행까지만 - 운영팀이 수동으로 조회/처리)
     */
    fun saveToDlq(
        originalTopic: String,
        messagePayload: String,
        consumerGroup: String,
        eventType: String? = null,
        exception: Exception? = null,
    ): DlqMessage {
        val dlqMessage = DlqMessage(
            originalTopic = originalTopic,
            messagePayload = messagePayload,
            consumerGroup = consumerGroup,
            eventType = eventType,
            errorMessage = exception?.message,
            errorStackTrace = exception?.stackTraceToString(),
            status = DlqStatus.PENDING,
        )

        val saved = dlqMessageRepository.save(dlqMessage)
        logger.warn(
            "Message saved to DLQ: topic={}, consumerGroup={}, dlqId={}, error={}",
            originalTopic,
            consumerGroup,
            saved.id,
            exception?.message,
        )

        return saved
    }
}
