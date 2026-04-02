package com.loopers.infrastructure.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox")
class OutboxEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val aggregateId: Long,
    val eventType: String,
    @Column(columnDefinition = "JSON")
    val payload: String,
    val topic: String = "metrics-events",

    // Kafka 파티션 키 (동시성 제어용, nullable)
    // - null: aggregateId를 key로 사용
    // - "userId:templateId": 같은 사용자의 같은 템플릿 요청은 같은 파티션으로 라우팅
    @Column(nullable = true)
    val partitionKey: String? = null,

    var published: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var publishedAt: LocalDateTime? = null,
)
