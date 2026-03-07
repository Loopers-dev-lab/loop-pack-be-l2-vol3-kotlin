package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
@Comment("쿠폰 템플릿")
class Coupon(
    name: String,
    type: CouponType,
    value: Long,
    expiredAt: ZonedDateTime,
) : BaseEntity() {

    @Comment("쿠폰명")
    @Column(nullable = false)
    var name: String = name
        protected set

    @Comment("쿠폰 유형 (FIXED: 정액 / RATE: 정률)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: CouponType = type
        protected set

    @Comment("할인 값 (FIXED: 원 / RATE: %)")
    @Column(nullable = false)
    var value: Long = value
        protected set

    @Comment("만료 일시")
    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    init {
        validate(name, type, value, expiredAt)
    }

    fun update(
        name: String,
        type: CouponType,
        value: Long,
        expiredAt: ZonedDateTime,
    ) {
        validate(name, type, value, expiredAt)
        this.name = name
        this.type = type
        this.value = value
        this.expiredAt = expiredAt
    }

    fun isExpired(): Boolean = ZonedDateTime.now().isAfter(expiredAt)

    /**
     * 주문 금액에 대한 할인 금액을 계산한다.
     *
     * - FIXED: 정액 할인. 주문 금액을 초과하지 않도록 min 처리
     * - RATE: 정률 할인. BigDecimal로 정밀 계산, 소수점 이하 내림(FLOOR)
     *   → 내림 이유: 할인 금액을 올림하면 고객에게 과다 할인 → 비즈니스 손실
     */
    fun calculateDiscount(orderAmount: Money): Money {
        if (orderAmount == Money.ZERO) return Money.ZERO

        return when (type) {
            CouponType.FIXED -> Money(minOf(value, orderAmount.amount))
            CouponType.RATE -> {
                val discount = BigDecimal(orderAmount.amount)
                    .multiply(BigDecimal(value))
                    .divide(BigDecimal(100), 0, RoundingMode.DOWN)
                    .toLong()
                Money(discount)
            }
        }
    }

    fun softDelete() {
        delete()
    }

    private fun validate(name: String, type: CouponType, value: Long, expiredAt: ZonedDateTime) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다.")
        }
        if (value <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.")
        }
        if (type == CouponType.RATE && value > 100) {
            throw CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.")
        }
    }
}
