package com.loopers.infrastructure.coupon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {

    @Modifying
    @Query("UPDATE CouponEntity c SET c.issuedCount = c.issuedCount + 1 WHERE c.id = :id")
    fun incrementIssuedCount(id: Long): Int
}
