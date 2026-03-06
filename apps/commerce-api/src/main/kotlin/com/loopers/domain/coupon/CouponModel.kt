package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon")
class CouponModel(
    name: String,
    type: CouponType,
    value: Long,
    minOrderAmount: Long? = null,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: CouponType = type
        protected set

    @Column(nullable = false)
    var value: Long = value
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    init {
        validateName(name)
        validateValue(type, value)
    }

    fun update(
        name: String,
        type: CouponType,
        value: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ) {
        validateName(name)
        validateValue(type, value)
        this.name = name
        this.type = type
        this.value = value
        this.minOrderAmount = minOrderAmount
        this.expiredAt = expiredAt
    }

    fun calculateDiscount(orderAmount: Long): Long {
        validateMinOrderAmount(orderAmount)
        return when (type) {
            CouponType.FIXED -> value.coerceAtMost(orderAmount)
            CouponType.RATE -> orderAmount * value / 100
        }
    }

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다.")
        }
        if (name.length > 100) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 100자 이하여야 합니다.")
        }
    }

    private fun validateValue(type: CouponType, value: Long) {
        if (value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 값은 1 이상이어야 합니다.")
        }
        if (type == CouponType.RATE && value > 100) {
            throw CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.")
        }
    }

    private fun validateMinOrderAmount(orderAmount: Long) {
        minOrderAmount?.let {
            if (orderAmount < it) {
                throw CoreException(
                    ErrorType.BAD_REQUEST,
                    "최소 주문 금액 조건을 충족하지 않습니다. (최소: ${it}원, 주문: ${orderAmount}원)",
                )
            }
        }
    }
}
