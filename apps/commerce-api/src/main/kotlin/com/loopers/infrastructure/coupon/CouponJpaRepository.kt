package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CouponJpaRepository : JpaRepository<CouponModel, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponModel?
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<CouponModel>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponModel>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponModel c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findByIdForUpdate(@Param("id") id: Long): CouponModel?
}
