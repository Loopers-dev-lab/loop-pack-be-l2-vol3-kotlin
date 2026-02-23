package com.loopers.infrastructure.point

import com.loopers.domain.BaseEntity
import com.loopers.domain.point.entity.UserPoint
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
                refUserId = userPoint.refUserId,
                balance = userPoint.balance,
            ).also { entity ->
                if (userPoint.id != 0L) {
                    setBaseEntityField(entity, "id", userPoint.id)
                    setBaseEntityField(entity, "createdAt", userPoint.createdAt)
                    setBaseEntityField(entity, "updatedAt", userPoint.updatedAt)
                }
            }
        }

        private fun setBaseEntityField(entity: BaseEntity, fieldName: String, value: Any) {
            BaseEntity::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(entity, value)
            }
        }
    }

    fun toDomain(): UserPoint {
        val up = UserPoint(refUserId = refUserId, balance = balance)
        UserPoint::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(up, id)
        }
        UserPoint::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(up, createdAt)
        }
        UserPoint::class.java.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(up, updatedAt)
        }
        return up
    }
}
