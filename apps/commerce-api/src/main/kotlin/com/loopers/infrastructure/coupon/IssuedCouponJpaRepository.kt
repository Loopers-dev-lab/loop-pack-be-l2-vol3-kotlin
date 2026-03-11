package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface IssuedCouponJpaRepository : JpaRepository<IssuedCouponEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<IssuedCouponEntity>
    fun existsByCouponIdAndMemberId(couponId: Long, memberId: Long): Boolean
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM IssuedCouponEntity i WHERE i.id = :id")
    fun findByIdForUpdate(id: Long): IssuedCouponEntity?
}
