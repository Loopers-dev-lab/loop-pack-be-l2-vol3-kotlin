package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponTemplate
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CouponTemplateJpaRepository : JpaRepository<CouponTemplate, Long> {

    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.deletedAt IS NULL ORDER BY ct.createdAt DESC")
    fun findActiveTemplates(pageable: Pageable): Page<CouponTemplate>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ct FROM CouponTemplate ct WHERE ct.id = :id")
    fun findByIdForUpdate(id: Long): CouponTemplate?

    @Modifying
    @Query(
        """
        UPDATE CouponTemplate ct
        SET ct.issuedCount = ct.issuedCount + 1
        WHERE ct.id = :id AND (ct.totalCount IS NULL OR ct.issuedCount < ct.totalCount)
        """,
    )
    fun incrementIssuedCountIfAvailable(id: Long): Int
}
