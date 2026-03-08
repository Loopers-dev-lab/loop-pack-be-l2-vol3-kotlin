package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupons",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "template_id"], name = "uk_coupon_user_template"),
    ],
)
class Coupon private constructor(
    userId: Long,
    templateId: Long,
) : BaseEntity() {

    @Column(nullable = false)
    val userId: Long = userId

    @Column(nullable = false)
    val templateId: Long = templateId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CouponStatus = CouponStatus.ISSUED
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    fun isValid(): Boolean {
        if (status == CouponStatus.USED) {
            return false
        }
        return true
    }

    fun canApplyToOrder(orderAmount: BigDecimal): Boolean {
        // 상태가 유효한지 확인 (사용되지 않은 상태)
        return isValid()
    }

    companion object {
        fun issue(userId: Long, template: CouponTemplate): Coupon {
            return Coupon(
                userId = userId,
                templateId = template.id,
            )
        }
    }
}
