package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "coupon_issue",
    uniqueConstraints = [UniqueConstraint(columnNames = ["coupon_id", "user_id"])],
)
class CouponIssueModel(
    couponId: Long,
    userId: Long,
    status: CouponIssueStatus = CouponIssueStatus.AVAILABLE,
) : BaseEntity() {

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CouponIssueStatus = status
        protected set

    fun use() {
        if (status != CouponIssueStatus.AVAILABLE) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "사용할 수 없는 쿠폰입니다. (현재 상태: $status)",
            )
        }
        this.status = CouponIssueStatus.USED
    }

    fun isUsable(): Boolean = status == CouponIssueStatus.AVAILABLE
}
