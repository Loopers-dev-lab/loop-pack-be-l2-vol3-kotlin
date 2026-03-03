package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "issued_coupons")
class IssuedCouponEntity(
    @Column(name = "ref_coupon_id", nullable = false)
    var refCouponId: Long,
    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: IssuedCoupon.CouponStatus,
    @Column(name = "used_at")
    var usedAt: ZonedDateTime?,
    @Column(name = "issued_at", nullable = false)
    var issuedAt: ZonedDateTime,
) : BaseEntity() {

    companion object {
        fun fromDomain(issuedCoupon: IssuedCoupon): IssuedCouponEntity {
            return IssuedCouponEntity(
                refCouponId = issuedCoupon.refCouponId.value,
                refUserId = issuedCoupon.refUserId.value,
                status = issuedCoupon.status,
                usedAt = issuedCoupon.usedAt,
                issuedAt = issuedCoupon.createdAt,
            ).withBaseFields(
                id = issuedCoupon.id,
            )
        }
    }

    fun toDomain(): IssuedCoupon = IssuedCoupon(
        id = id,
        refCouponId = CouponId(refCouponId),
        refUserId = UserId(refUserId),
        status = status,
        usedAt = usedAt,
        createdAt = issuedAt,
    )
}
