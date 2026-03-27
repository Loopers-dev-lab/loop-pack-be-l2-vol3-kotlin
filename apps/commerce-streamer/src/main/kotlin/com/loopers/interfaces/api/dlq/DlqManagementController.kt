package com.loopers.interfaces.api.dlq

import com.loopers.domain.dlq.DlqMessage
import com.loopers.domain.dlq.DlqStatus
import com.loopers.infrastructure.dlq.DlqMessageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * DLQ(Dead Letter Queue) 조회 API
 *
 * 운영팀이 실패한 메시지를 모니터링할 수 있는 조회 전용 API
 * (발행/저장만 구현 - 자동 재시도 및 수동 처리는 미포함)
 */
@RestController
@RequestMapping("/api/v1/admin/dlq")
class DlqManagementController(
    private val dlqMessageRepository: DlqMessageRepository,
) {

    /**
     * DLQ 메시지 목록 조회
     */
    @GetMapping("/messages")
    fun listDlqMessages(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String? = null,
    ): Page<DlqMessage> {
        val pageable = PageRequest.of(page, size)

        return if (status != null) {
            val dlqStatus = DlqStatus.valueOf(status.uppercase())
            dlqMessageRepository.findByStatus(dlqStatus, pageable)
        } else {
            dlqMessageRepository.findAll(pageable)
        }
    }

    /**
     * DLQ 메시지 상세 조회
     */
    @GetMapping("/messages/{dlqId}")
    fun getDlqMessage(
        @PathVariable dlqId: Long,
    ): DlqMessage {
        return dlqMessageRepository.findById(dlqId)
            .orElseThrow { IllegalArgumentException("DLQ message not found: $dlqId") }
    }

    /**
     * 특정 토픽의 DLQ 통계
     */
    @GetMapping("/stats")
    fun getDlqStats(
        @RequestParam topic: String,
    ): Map<String, Any> {
        val messages = dlqMessageRepository.findByOriginalTopic(topic, PageRequest.of(0, Int.MAX_VALUE))
        val pending = messages.content.count { it.status == DlqStatus.PENDING }
        val deadLettered = messages.content.count { it.status == DlqStatus.DEAD_LETTERED }
        val resolved = messages.content.count { it.status == DlqStatus.RESOLVED }

        return mapOf(
            "topic" to topic,
            "total" to messages.totalElements,
            "pending" to pending,
            "deadLettered" to deadLettered,
            "resolved" to resolved,
        )
    }

    /**
     * DLQ 전체 통계
     */
    @GetMapping("/summary")
    fun getDlqSummary(): Map<String, Long> {
        return mapOf(
            "pending" to dlqMessageRepository.countByStatus(DlqStatus.PENDING),
            "deadLettered" to dlqMessageRepository.countByStatus(DlqStatus.DEAD_LETTERED),
            "resolved" to dlqMessageRepository.countByStatus(DlqStatus.RESOLVED),
        )
    }
}
