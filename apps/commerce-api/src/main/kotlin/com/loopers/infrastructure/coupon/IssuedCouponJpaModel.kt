package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.IssuedCouponModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.ZonedDateTime

@Entity
@Table(name = "issued_coupon")
class IssuedCouponJpaModel(
    couponTemplateId: Long,
    memberId: Long,
    expiredAt: ZonedDateTime,
) : BaseEntity() {
    @Column(name = "coupon_template_id", nullable = false)
    var couponTemplateId: Long = couponTemplateId
        protected set

    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponStatus = CouponStatus.AVAILABLE
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0

    fun toModel(): IssuedCouponModel = IssuedCouponModel(
        id = id,
        couponTemplateId = couponTemplateId,
        memberId = memberId,
        status = status,
        expiredAt = expiredAt,
        usedAt = usedAt,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun updateFrom(model: IssuedCouponModel) {
        this.status = model.status
        this.usedAt = model.usedAt
    }

    companion object {
        fun from(model: IssuedCouponModel): IssuedCouponJpaModel =
            IssuedCouponJpaModel(
                couponTemplateId = model.couponTemplateId,
                memberId = model.memberId,
                expiredAt = model.expiredAt,
            )
    }
}
