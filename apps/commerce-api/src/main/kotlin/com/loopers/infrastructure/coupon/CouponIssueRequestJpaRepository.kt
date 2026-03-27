package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponIssueRequestJpaRepository : JpaRepository<CouponIssueRequestEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponIssueRequestEntity?

    fun findByIdAndUserIdAndDeletedAtIsNull(id: Long, userId: Long): CouponIssueRequestEntity?

    fun findByCouponIdAndUserIdAndDeletedAtIsNull(couponId: Long, userId: Long): CouponIssueRequestEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CouponIssueRequestEntity r where r.id = :id and r.deletedAt is null")
    fun findByIdForUpdate(@Param("id") id: Long): CouponIssueRequestEntity?
}
