package com.loopers.infrastructure.coupon

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Table(
    name = "issued_coupon",
    indexes = [
        Index(columnList = "user_id, deleted_at"),
        Index(columnList = "coupon_id, deleted_at"),
    ],
)
@Entity
class IssuedCouponEntity(
    id: Long? = null,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(nullable = false)
    val status: String,
    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime,
    @Column(name = "used_at")
    val usedAt: ZonedDateTime?,
    @Column(nullable = false)
    val version: Long = 0L,
) : BaseEntity() {

    init {
        this.id = id
    }
}
