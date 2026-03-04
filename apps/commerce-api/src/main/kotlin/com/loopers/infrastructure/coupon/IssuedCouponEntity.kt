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
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_issued_coupons_coupon_user", columnNames = ["ref_coupon_id", "ref_user_id"]),
    ],
    indexes = [
        Index(name = "idx_issued_coupons_ref_user_id", columnList = "ref_user_id"),
    ],
)
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
) : BaseEntity() {

    companion object {
        fun fromDomain(issuedCoupon: IssuedCoupon): IssuedCouponEntity {
            return IssuedCouponEntity(
                refCouponId = issuedCoupon.refCouponId.value,
                refUserId = issuedCoupon.refUserId.value,
                status = issuedCoupon.status,
                usedAt = issuedCoupon.usedAt,
            ).withBaseFields(
                id = issuedCoupon.id,
                createdAt = issuedCoupon.createdAt,
            )
        }
    }

    fun toDomain(): IssuedCoupon = IssuedCoupon(
        id = id,
        refCouponId = CouponId(refCouponId),
        refUserId = UserId(refUserId),
        status = status,
        usedAt = usedAt,
        createdAt = createdAt,
    )
}
