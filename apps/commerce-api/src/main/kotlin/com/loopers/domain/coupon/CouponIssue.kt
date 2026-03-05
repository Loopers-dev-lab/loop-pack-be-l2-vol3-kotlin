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
import org.hibernate.annotations.Comment
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupon_issues",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "coupon_id"])],
)
@Comment("발급된 쿠폰")
class CouponIssue(
    couponId: Long,
    userId: Long,
) : BaseEntity() {

    @Comment("쿠폰 템플릿")
    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Comment("발급 대상 회원")
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    /**
     * 낙관적 락 버전.
     *
     * 왜 @Version인가?
     * - CouponIssue는 독립 엔티티 → false contention 없음 (Product와 다름)
     * - 경합 극히 낮음 (같은 유저 중복 요청뿐) → 재시도 거의 불필요
     * - JPA dirty checking과 자연스럽게 동작 → couponIssue.use() 엔티티 메서드 유지
     * - 충돌 시 ObjectOptimisticLockingFailureException → "이미 사용된 쿠폰" 처리
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    @Comment("쿠폰 상태 (AVAILABLE/USED/EXPIRED)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CouponIssueStatus = CouponIssueStatus.AVAILABLE
        protected set

    @Comment("사용 일시")
    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    init {
        if (couponId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 쿠폰입니다.")
        }
        if (userId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 회원입니다.")
        }
    }

    /**
     * 쿠폰을 사용 처리한다.
     * 만료 여부는 Coupon.expiredAt 시간 비교로 판단하므로,
     * 여기서는 status 기반 검증만 수행한다.
     */
    fun use() {
        if (status != CouponIssueStatus.AVAILABLE) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
        }
        this.status = CouponIssueStatus.USED
        this.usedAt = ZonedDateTime.now()
    }

    fun isUsable(): Boolean = status == CouponIssueStatus.AVAILABLE

    fun expire() {
        if (status == CouponIssueStatus.AVAILABLE) {
            this.status = CouponIssueStatus.EXPIRED
        }
    }
}
