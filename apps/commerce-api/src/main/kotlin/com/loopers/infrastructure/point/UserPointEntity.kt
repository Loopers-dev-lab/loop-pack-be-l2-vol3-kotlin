package com.loopers.infrastructure.point

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.vo.Point
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user_points")
class UserPointEntity(
    @Column(name = "ref_user_id", nullable = false, unique = true)
    var refUserId: Long,
    @Column(name = "balance", nullable = false)
    var balance: Long,
) : BaseEntity() {

    companion object {
        fun fromDomain(userPoint: UserPoint): UserPointEntity {
            return UserPointEntity(
                refUserId = userPoint.refUserId.value,
                balance = userPoint.balance.value,
            ).withBaseFields(
                id = userPoint.id,
            )
        }
    }

    fun toDomain(): UserPoint = UserPoint(
        id = id,
        refUserId = UserId(refUserId),
        balance = Point(balance),
    )
}
