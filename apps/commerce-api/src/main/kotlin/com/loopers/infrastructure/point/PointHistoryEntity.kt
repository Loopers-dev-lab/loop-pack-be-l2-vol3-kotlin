package com.loopers.infrastructure.point

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.model.PointHistory.PointHistoryType
import com.loopers.domain.point.vo.Point
import com.loopers.domain.withBaseFields
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
                amount = pointHistory.amount.value,
                refOrderId = pointHistory.refOrderId?.value,
            ).withBaseFields(
                id = pointHistory.id,
            )
        }
    }

    fun toDomain(): PointHistory {
        val ph = PointHistory(
            refUserPointId = refUserPointId,
            type = type,
            amount = Point(amount),
            refOrderId = refOrderId?.let { OrderId(it) },
        )
        PointHistory::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(ph, id)
        }
        return ph
    }
}
