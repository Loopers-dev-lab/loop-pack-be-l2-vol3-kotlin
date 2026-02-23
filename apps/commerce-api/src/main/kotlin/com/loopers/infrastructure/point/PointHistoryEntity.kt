package com.loopers.infrastructure.point

import com.loopers.domain.BaseEntity
import com.loopers.domain.point.entity.PointHistory
import com.loopers.domain.point.entity.PointHistory.PointHistoryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "point_histories")
class PointHistoryEntity(
    @Column(name = "ref_user_point_id", nullable = false)
    var refUserPointId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: PointHistoryType,
    @Column(name = "amount", nullable = false)
    var amount: Long,
    @Column(name = "ref_order_id")
    var refOrderId: Long?,
) : BaseEntity() {

    companion object {
        fun fromDomain(pointHistory: PointHistory): PointHistoryEntity {
            return PointHistoryEntity(
                refUserPointId = pointHistory.refUserPointId,
                type = pointHistory.type,
                amount = pointHistory.amount,
                refOrderId = pointHistory.refOrderId,
            ).also { entity ->
                if (pointHistory.id != 0L) {
                    setBaseEntityField(entity, "id", pointHistory.id)
                    setBaseEntityField(entity, "createdAt", pointHistory.createdAt)
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

    fun toDomain(): PointHistory {
        val ph = PointHistory(
            refUserPointId = refUserPointId,
            type = type,
            amount = amount,
            refOrderId = refOrderId,
        )
        PointHistory::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(ph, id)
        }
        PointHistory::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(ph, createdAt)
        }
        return ph
    }
}
