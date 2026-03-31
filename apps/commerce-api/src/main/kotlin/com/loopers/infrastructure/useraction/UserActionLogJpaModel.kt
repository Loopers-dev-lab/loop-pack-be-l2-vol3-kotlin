package com.loopers.infrastructure.useraction

import com.loopers.domain.useraction.UserActionLogModel
import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "user_action_log")
class UserActionLogJpaModel(
    memberId: Long,
    actionType: UserActionType,
    targetType: UserActionTargetType,
    targetId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var actionType: UserActionType = actionType
        protected set

    @Column(name = "target_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var targetType: UserActionTargetType = targetType
        protected set

    @Column(name = "target_id", nullable = false)
    var targetId: Long = targetId
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }

    fun toModel(): UserActionLogModel = UserActionLogModel(
        id = id,
        memberId = memberId,
        actionType = actionType,
        targetType = targetType,
        targetId = targetId,
        createdAt = createdAt,
    )

    companion object {
        fun from(model: UserActionLogModel): UserActionLogJpaModel =
            UserActionLogJpaModel(
                memberId = model.memberId,
                actionType = model.actionType,
                targetType = model.targetType,
                targetId = model.targetId,
            )
    }
}
