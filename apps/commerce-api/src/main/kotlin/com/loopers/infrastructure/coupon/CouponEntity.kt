package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.DiscountType
import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon")
class CouponEntity(
    id: Long?,

    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    val discountType: DiscountType,

    @Column(name = "discount_value", nullable = false)
    val discountValue: Long,

    @Column(name = "min_order_amount", nullable = false)
    val minOrderAmount: Long,

    @Column(name = "max_issue_count")
    val maxIssueCount: Int?,

    @Column(name = "issued_count", nullable = false)
    val issuedCount: Int,

    @Column(name = "expired_at", nullable = false)
    val expiredAt: ZonedDateTime,

    deletedAt: ZonedDateTime?,
) : BaseEntity() {
    init {
        this.id = id
        if (deletedAt != null) {
            this.deletedAt = deletedAt
        }
    }
}
