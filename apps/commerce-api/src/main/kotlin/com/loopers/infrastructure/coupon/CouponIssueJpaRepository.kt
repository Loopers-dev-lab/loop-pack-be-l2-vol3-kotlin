package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponIssueJpaRepository : JpaRepository<CouponIssueModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueModel?
    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): CouponIssueModel?
    fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<CouponIssueModel>
    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long, pageable: Pageable): Page<CouponIssueModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ci FROM CouponIssueModel ci WHERE ci.id = :id AND ci.deletedAt IS NULL")
    fun findByIdForUpdate(@Param("id") id: Long): CouponIssueModel?
}
