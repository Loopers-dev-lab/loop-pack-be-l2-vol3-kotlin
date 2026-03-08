package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface CouponJpaRepository : JpaRepository<Coupon, Long> {

    @Query(
        "SELECT c FROM Coupon c WHERE c.userId = :userId AND c.templateId = :templateId",
    )
    fun findByUserIdAndTemplateId(userId: Long, templateId: Long): Coupon?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT c FROM Coupon c WHERE c.userId = :userId AND c.templateId = :templateId",
    )
    fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): Coupon?

    @Query("SELECT c FROM Coupon c WHERE c.userId = :userId ORDER BY c.createdAt DESC")
    fun findByUserId(userId: Long, pageable: Pageable): Page<Coupon>

    @Query(
        "SELECT c FROM Coupon c WHERE c.userId = :userId AND c.status = :status ORDER BY c.createdAt DESC",
    )
    fun findByUserIdAndStatus(userId: Long, status: CouponStatus, pageable: Pageable): Page<Coupon>

    @Query("SELECT c FROM Coupon c WHERE c.templateId = :templateId ORDER BY c.createdAt DESC")
    fun findByTemplateId(templateId: Long, pageable: Pageable): Page<Coupon>

    /**
     * 쿠폰 상태를 원자적으로 ISSUED에서 USED로 변경
     * ISSUED 상태일 때만 USED로 업데이트됨 (동시성 제어)
     *
     * @param couponId 쿠폰 ID
     * @return 업데이트된 행 수 (0 또는 1)
     */
    @Modifying
    @Transactional
    @Query(
        """
        UPDATE Coupon c
        SET c.status = 'USED', c.usedAt = CURRENT_TIMESTAMP
        WHERE c.id = :couponId AND c.status = 'ISSUED'
        """,
    )
    fun updateStatusToUsed(couponId: Long): Int
}
