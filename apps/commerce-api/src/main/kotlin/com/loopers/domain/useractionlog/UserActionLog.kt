package com.loopers.domain.useractionlog

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "user_action_logs",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_action_logs_dedupe_key", columnNames = ["dedupe_key"])],
    indexes = [
        Index(name = "idx_user_action_logs_partition_date", columnList = "partition_date"),
        Index(name = "idx_user_action_logs_action_type", columnList = "action_type"),
    ],
)
class UserActionLog protected constructor(
    @Column(name = "action_type", nullable = false, length = 100)
    val actionType: String,
    @Column(name = "actor_user_id", nullable = false)
    val actorUserId: Long,
    @Column(name = "target_id", nullable = false, length = 120)
    val targetId: String,
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Column(name = "dedupe_key", nullable = false, length = 200)
    val dedupeKey: String,
    @Column(name = "partition_date", nullable = false)
    val partitionDate: LocalDate,
) : BaseEntity() {
    companion object {
        fun append(command: UserActionLogAppendCommand): UserActionLog =
            UserActionLog(
                actionType = command.actionType,
                actorUserId = command.actorUserId,
                targetId = command.targetId,
                payload = command.payload,
                dedupeKey = command.dedupeKey,
                partitionDate = command.partitionDate,
            )
    }
}
