package com.loopers.infrastructure.coupon

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    fun findAllByIdIn(ids: List<Long>): List<CouponEntity>

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE CouponEntity c
        SET c.issuedCount = c.issuedCount + 1
        WHERE c.id = :id
          AND (c.issueLimit IS NULL OR c.issuedCount < c.issueLimit)
        """,
    )
    fun tryIncreaseIssuedCount(@Param("id") id: Long): Int
}
