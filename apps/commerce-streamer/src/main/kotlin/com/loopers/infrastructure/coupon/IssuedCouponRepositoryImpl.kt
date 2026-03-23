package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.repository.IssuedCouponRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Entity
@Table(
    name = "issued_coupons",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_issued_coupons_coupon_user", columnNames = ["ref_coupon_id", "ref_user_id"]),
    ],
    indexes = [
        Index(name = "idx_issued_coupons_ref_user_id", columnList = "ref_user_id"),
        Index(name = "idx_issued_coupons_ref_coupon_id", columnList = "ref_coupon_id"),
    ],
)
class IssuedCouponEntity(
    @Column(name = "ref_coupon_id", nullable = false)
    var refCouponId: Long,
    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long,
    @Column(name = "status", nullable = false)
    var status: String,
    @Column(name = "used_at")
    var usedAt: ZonedDateTime?,
) : BaseEntity()

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun existsByRefCouponIdAndRefUserId(refCouponId: Long, refUserId: Long): Boolean
}

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
) : IssuedCouponRepository {

    override fun existsByRefCouponIdAndRefUserId(couponId: Long, userId: Long): Boolean =
        issuedCouponJpaRepository.existsByRefCouponIdAndRefUserId(couponId, userId)

    override fun save(refCouponId: Long, refUserId: Long) {
        issuedCouponJpaRepository.save(
            IssuedCouponEntity(
                refCouponId = refCouponId,
                refUserId = refUserId,
                status = "AVAILABLE",
                usedAt = null,
            ),
        )
    }
}
