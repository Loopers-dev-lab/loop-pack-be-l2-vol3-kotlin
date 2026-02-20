package com.loopers.domain.point.entity

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "point_histories")
class PointHistory(
    refUserPointId: Long,
    type: PointHistoryType,
    amount: Long,
    refOrderId: Long? = null,
) {

    init {
        if (amount <= 0) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "포인트 이력 금액은 0보다 커야 합니다.",
            )
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "ref_user_point_id", nullable = false)
    var refUserPointId: Long = refUserPointId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: PointHistoryType = type
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = amount
        protected set

    @Column(name = "ref_order_id")
    var refOrderId: Long? = refOrderId
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}
