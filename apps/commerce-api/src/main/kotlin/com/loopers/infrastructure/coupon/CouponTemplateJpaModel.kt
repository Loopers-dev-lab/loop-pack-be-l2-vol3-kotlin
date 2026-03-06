package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.CouponTemplateModel
import com.loopers.domain.coupon.CouponTemplateStatus
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.ExpirationPolicy
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon_template")
class CouponTemplateJpaModel(
    name: String,
    type: CouponType,
    value: Long,
    minOrderAmount: Long?,
    maxDiscountAmount: Long?,
    expirationPolicy: ExpirationPolicy,
    expiredAt: ZonedDateTime?,
    validDays: Int?,
) : BaseEntity() {
    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: CouponType = type
        protected set

    @Column(name = "value", nullable = false)
    var value: Long = value
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "max_discount_amount")
    var maxDiscountAmount: Long? = maxDiscountAmount
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "expiration_policy", nullable = false, length = 30)
    var expirationPolicy: ExpirationPolicy = expirationPolicy
        protected set

    @Column(name = "expired_at")
    var expiredAt: ZonedDateTime? = expiredAt
        protected set

    @Column(name = "valid_days")
    var validDays: Int? = validDays
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CouponTemplateStatus = CouponTemplateStatus.ACTIVE
        protected set

    fun toModel(): CouponTemplateModel = CouponTemplateModel(
        id = id,
        name = name,
        type = type,
        value = value,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        expirationPolicy = expirationPolicy,
        expiredAt = expiredAt,
        validDays = validDays,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: CouponTemplateModel) {
        this.name = model.name
        this.type = model.type
        this.value = model.value
        this.minOrderAmount = model.minOrderAmount
        this.maxDiscountAmount = model.maxDiscountAmount
        this.expirationPolicy = model.expirationPolicy
        this.expiredAt = model.expiredAt
        this.validDays = model.validDays
        this.status = model.status
        if (model.deletedAt != null) {
            this.deletedAt = model.deletedAt
        }
    }

    companion object {
        fun from(model: CouponTemplateModel): CouponTemplateJpaModel =
            CouponTemplateJpaModel(
                name = model.name,
                type = model.type,
                value = model.value,
                minOrderAmount = model.minOrderAmount,
                maxDiscountAmount = model.maxDiscountAmount,
                expirationPolicy = model.expirationPolicy,
                expiredAt = model.expiredAt,
                validDays = model.validDays,
            )
    }
}
