package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.model.Coupon
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupons",
    indexes = [
        Index(name = "idx_coupons_expired_at_deleted_at", columnList = "expired_at, deleted_at"),
    ],
)
class CouponEntity(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "type", nullable = false)
    var type: String,
    @Column(name = "value", nullable = false)
    var value: Long,
    @Column(name = "max_discount", precision = 10, scale = 2)
    var maxDiscount: BigDecimal?,
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    var minOrderAmount: BigDecimal?,
    @Column(name = "total_quantity")
    var totalQuantity: Int?,
    @Column(name = "issued_count", nullable = false)
    var issuedCount: Int,
    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime,
) : BaseEntity() {

    fun toDomain(): Coupon = Coupon(
        id = id,
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        expiredAt = expiredAt,
        deletedAt = deletedAt,
    )
}
