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
import jakarta.persistence.Version
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(
    name = "tb_user_coupon",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_template_id"])],
)
@SQLRestriction("deleted_at IS NULL")
class UserCouponModel(
    userId: Long,
    couponTemplateId: Long,
    status: UserCouponStatus,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "coupon_template_id", nullable = false)
    var couponTemplateId: Long = couponTemplateId
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserCouponStatus = status
        protected set

    @Version
    var version: Long = 0
        protected set

    fun use() {
        if (status != UserCouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 가능한 상태의 쿠폰이 아닙니다.")
        }
        status = UserCouponStatus.USED
    }

    fun expire() {
        if (status != UserCouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 가능한 상태의 쿠폰만 만료 처리할 수 있습니다.")
        }
        status = UserCouponStatus.EXPIRED
    }
}
