package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueRequestModel
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueRequestModel?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cir FROM CouponIssueRequestModel cir WHERE cir.id = :id AND cir.deletedAt IS NULL")
    fun findByIdForUpdate(@Param("id") id: Long): CouponIssueRequestModel?

    fun findAllByCouponIdAndDeletedAtIsNull(couponId: Long): List<CouponIssueRequestModel>
}
