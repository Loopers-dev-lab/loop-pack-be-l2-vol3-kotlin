package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponJpaRepository : JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findActiveByIdOrNull(@Param("id") id: Long): Coupon?

    @Query("SELECT c FROM Coupon c WHERE c.deletedAt IS NULL")
    fun findAllActive(pageable: Pageable): Page<Coupon>
}
