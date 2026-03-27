package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<Coupon, Long> {

    fun findByIdAndDeletedAtIsNull(id: Long): Coupon?

    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<Coupon>

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Coupon>

    /**
     * 발급 수량 atomic increment.
     * max_issue_count가 null(무제한)이거나 issued_count < max_issue_count일 때만 증가.
     */
    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Coupon c
        SET c.issuedCount = c.issuedCount + 1
        WHERE c.id = :couponId
        AND c.deletedAt IS NULL
        AND (c.maxIssueCount IS NULL OR c.issuedCount < c.maxIssueCount)
        """,
    )
    fun incrementIssuedCount(couponId: Long): Int
}
