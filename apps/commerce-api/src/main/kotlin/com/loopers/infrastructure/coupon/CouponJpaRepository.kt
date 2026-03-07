package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponEntity c WHERE c.id = :id")
    fun findByIdForUpdate(id: Long): CouponEntity?

    @Modifying
    @Query(
        "UPDATE CouponEntity c SET c.issuedCount = c.issuedCount + 1 " +
            "WHERE c.id = :id AND (c.maxIssueCount IS NULL OR c.issuedCount < c.maxIssueCount)",
    )
    fun incrementIssuedCount(id: Long): Int
}
