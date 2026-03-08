package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "coupon_templates")
class CouponTemplate private constructor(
    name: String,
    type: CouponType,
    value: BigDecimal,
    minOrderAmount: BigDecimal,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Column(nullable = false, unique = true, length = 100)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: CouponType = type
        protected set

    @Column(nullable = false, precision = 19, scale = 2)
    var value: BigDecimal = value
        protected set

    @Column(nullable = false, precision = 19, scale = 2)
    var minOrderAmount: BigDecimal = minOrderAmount
        protected set

    @Column(nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    fun isApplicable(orderAmount: BigDecimal): Boolean {
        return orderAmount >= minOrderAmount && !isExpired()
    }

    fun isExpired(): Boolean {
        return ZonedDateTime.now().isAfter(expiredAt)
    }

    fun updateInfo(
        newName: String,
        newValue: BigDecimal,
        newMinOrderAmount: BigDecimal,
        newExpiredAt: ZonedDateTime,
    ) {
        validate(newName, newValue, newMinOrderAmount, newExpiredAt)
        this.name = newName
        this.value = newValue
        this.minOrderAmount = newMinOrderAmount
        this.expiredAt = newExpiredAt
        guard()
    }

    private fun validate(
        newName: String,
        newValue: BigDecimal,
        newMinOrderAmount: BigDecimal,
        newExpiredAt: ZonedDateTime,
    ) {
        if (newName.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 빈 값일 수 없습니다.")
        }
        if (newValue < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 음수일 수 없습니다.")
        }
        if (newMinOrderAmount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문액은 음수일 수 없습니다.")
        }
        if (newExpiredAt.isBefore(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효기간이 과거입니다.")
        }
    }

    override fun guard() {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 빈 값일 수 없습니다.")
        }
        if (value < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 음수일 수 없습니다.")
        }
        if (minOrderAmount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문액은 음수일 수 없습니다.")
        }
        if (expiredAt.isBefore(ZonedDateTime.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효기간이 과거입니다.")
        }
    }

    companion object {
        fun create(
            name: String,
            type: CouponType,
            value: BigDecimal,
            minOrderAmount: BigDecimal,
            expiredAt: ZonedDateTime,
        ): CouponTemplate {
            val template = CouponTemplate(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
            template.guard()
            return template
        }

        internal fun createForTest(
            name: String,
            type: CouponType,
            value: BigDecimal,
            minOrderAmount: BigDecimal,
            expiredAt: ZonedDateTime,
        ): CouponTemplate {
            return CouponTemplate(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )
        }
    }
}
