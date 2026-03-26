package com.loopers.domain.common

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.event.ActivityType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "user_activity_logs")
class UserActivityLogModel(
    userId: Long,
    loginId: String,
    activityType: ActivityType,
    targetId: Long,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "login_id", nullable = false)
    var loginId: String = loginId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    var activityType: ActivityType = activityType
        protected set

    @Column(name = "target_id", nullable = false)
    var targetId: Long = targetId
        protected set
}
