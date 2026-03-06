package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.ZonedDateTime

@Entity
@Table(name = "tb_coupon_template")
@SQLRestriction("deleted_at IS NULL")
class CouponTemplateModel(
    name: CouponName,
    type: CouponType,
    value: CouponValue,
    minOrderAmount: MinOrderAmount?,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(nullable = false)
    var name: CouponName = name
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: CouponType = type
        protected set

    @Column(nullable = false)
    var value: CouponValue = value
        protected set

    @Column(nullable = true)
    var minOrderAmount: MinOrderAmount? = minOrderAmount
        protected set

    @Column(nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    override fun guard() {
        if (type == CouponType.RATE) {
            require(value.value in 1..100) { "정률 쿠폰의 할인율은 1 이상 100 이하여야 합니다." }
        }
    }

    fun update(
        name: CouponName,
        type: CouponType,
        value: CouponValue,
        minOrderAmount: MinOrderAmount?,
        expiredAt: ZonedDateTime,
    ) {
        this.name = name
        this.type = type
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
    }

    fun calculate(orderAmount: Long): Long {
        minOrderAmount?.let {
            if (orderAmount < it.value) throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액 미충족")
        }
        return when (type) {
            CouponType.FIXED -> minOf(value.value, orderAmount)
            CouponType.RATE -> orderAmount * value.value / 100
        }
    }
}
