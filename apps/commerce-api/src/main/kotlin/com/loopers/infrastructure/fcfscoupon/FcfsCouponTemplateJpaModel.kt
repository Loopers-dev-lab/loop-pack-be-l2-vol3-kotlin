package com.loopers.infrastructure.fcfscoupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateModel
import com.loopers.domain.fcfscoupon.FcfsCouponTemplateStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "fcfs_coupon_template")
class FcfsCouponTemplateJpaModel(
    name: String,
    description: String?,
    discountType: CouponType,
    discountValue: Long,
    minOrderAmount: Long?,
    maxDiscountAmount: Long?,
    totalQuantity: Int,
    issuedQuantity: Int,
    status: FcfsCouponTemplateStatus,
    startedAt: ZonedDateTime,
    endedAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "description")
    var description: String? = description
        protected set

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var discountType: CouponType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Long = discountValue
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "max_discount_amount")
    var maxDiscountAmount: Long? = maxDiscountAmount
        protected set

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = totalQuantity
        protected set

    @Column(name = "issued_quantity", nullable = false)
    var issuedQuantity: Int = issuedQuantity
        protected set

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FcfsCouponTemplateStatus = status
        protected set

    @Column(name = "started_at", nullable = false)
    var startedAt: ZonedDateTime = startedAt
        protected set

    @Column(name = "ended_at", nullable = false)
    var endedAt: ZonedDateTime = endedAt
        protected set

    fun toModel(): FcfsCouponTemplateModel = FcfsCouponTemplateModel(
        id = id,
        name = name,
        description = description,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = maxDiscountAmount,
        totalQuantity = totalQuantity,
        issuedQuantity = issuedQuantity,
        status = status,
        startedAt = startedAt,
        endedAt = endedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: FcfsCouponTemplateModel) {
        name = model.name
        description = model.description
        discountType = model.discountType
        discountValue = model.discountValue
        minOrderAmount = model.minOrderAmount
        maxDiscountAmount = model.maxDiscountAmount
        totalQuantity = model.totalQuantity
        issuedQuantity = model.issuedQuantity
        status = model.status
        startedAt = model.startedAt
        endedAt = model.endedAt
        if (model.deletedAt != null) deletedAt = model.deletedAt
    }

    companion object {
        fun from(model: FcfsCouponTemplateModel): FcfsCouponTemplateJpaModel =
            FcfsCouponTemplateJpaModel(
                name = model.name,
                description = model.description,
                discountType = model.discountType,
                discountValue = model.discountValue,
                minOrderAmount = model.minOrderAmount,
                maxDiscountAmount = model.maxDiscountAmount,
                totalQuantity = model.totalQuantity,
                issuedQuantity = model.issuedQuantity,
                status = model.status,
                startedAt = model.startedAt,
                endedAt = model.endedAt,
            )
    }
}
