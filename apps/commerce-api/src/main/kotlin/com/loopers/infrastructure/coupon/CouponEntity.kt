package com.loopers.infrastructure.coupon

import com.loopers.infrastructure.AdminAuditEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Table(name = "coupon")
@Entity
class CouponEntity(
    id: Long? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    val type: String,
    @Column(name = "discount_value", nullable = false)
    var discountValue: Long,
    @Column(name = "min_order_amount")
    var minOrderAmount: BigDecimal?,
    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime,
    createdBy: String,
    updatedBy: String,
) : AdminAuditEntity() {

    init {
        this.id = id
        this.createdBy = createdBy
        this.updatedBy = updatedBy
    }
}
