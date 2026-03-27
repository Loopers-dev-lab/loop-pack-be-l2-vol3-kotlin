package com.loopers.domain.dlq

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "dlq_message")
class DlqMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val originalTopic: String,

    @Column(columnDefinition = "LONGTEXT")
    val messagePayload: String,

    val consumerGroup: String,

    val eventType: String? = null,

    @Column(columnDefinition = "TEXT")
    val errorMessage: String? = null,

    @Column(columnDefinition = "LONGTEXT")
    val errorStackTrace: String? = null,

    var retryCount: Int = 0,

    val maxRetries: Int = 3,

    var lastRetryAt: LocalDateTime? = null,

    var status: DlqStatus = DlqStatus.PENDING,

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime? = null,
)

enum class DlqStatus {
    PENDING, // 처리 대기
    RETRYING, // 재시도 중
    DEAD_LETTERED, // 최종 실패 (처리 불가)
    RESOLVED, // 수동 처리 완료
}
