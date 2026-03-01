package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface CouponJpaRepository : JpaRepository<Coupon, Long> {

    @Query(
        "SELECT c FROM Coupon c WHERE c.userId = :userId AND c.templateId = :templateId",
    )
    fun findByUserIdAndTemplateId(userId: Long, templateId: Long): Coupon?

    @Query("SELECT c FROM Coupon c WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    fun findByUserId(userId: Long, pageable: Pageable): Page<Coupon>

    @Query(
        "SELECT c FROM Coupon c WHERE c.userId = :userId AND c.status = :status ORDER BY c.createdAt DESC",
    )
    fun findByUserIdAndStatus(userId: Long, status: CouponStatus, pageable: Pageable): Page<Coupon>

    @Query("SELECT c FROM Coupon c WHERE c.templateId = :templateId ORDER BY c.createdAt DESC")
    fun findByTemplateId(templateId: Long, pageable: Pageable): Page<Coupon>
}
