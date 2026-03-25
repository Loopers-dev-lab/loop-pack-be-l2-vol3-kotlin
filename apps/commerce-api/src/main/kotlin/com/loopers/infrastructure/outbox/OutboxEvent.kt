package com.loopers.infrastructure.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "outbox_events")
class OutboxEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val aggregateType: String,

    @Column(nullable = false)
    val aggregateId: String,

    @Column(nullable = false)
    val eventType: String,

    @Column(nullable = false)
    val version: Long,

    @Column(columnDefinition = "TEXT", nullable = false)
    val payload: String,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    var publishedAt: Instant? = null,
)
