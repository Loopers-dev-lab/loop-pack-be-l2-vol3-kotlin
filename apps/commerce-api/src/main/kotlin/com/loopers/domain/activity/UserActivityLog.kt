package com.loopers.domain.activity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "user_activity_logs",
    indexes = [
        Index(name = "idx_activity_user_id", columnList = "user_id"),
        Index(name = "idx_activity_target", columnList = "target_type, target_id"),
    ],
)
class UserActivityLog(
    userId: Long,
    activityType: ActivityType,
    targetType: TargetType,
    targetId: Long,
    metadata: String? = null,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    val activityType: ActivityType = activityType

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    val targetType: TargetType = targetType

    @Column(name = "target_id", nullable = false)
    val targetId: Long = targetId

    @Column(name = "metadata", columnDefinition = "TEXT")
    val metadata: String? = metadata

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}

enum class ActivityType {
    VIEW,
    LIKE,
    UNLIKE,
    ORDER,
    PAYMENT,
    PAYMENT_FAILED,
}

enum class TargetType {
    PRODUCT,
    ORDER,
}
