package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface UserCouponJpaRepository : JpaRepository<UserCouponEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.id = :id")
    fun findByIdForUpdate(id: Long): UserCouponEntity?

    fun existsByCouponIdAndUserId(couponId: Long, userId: Long): Boolean

    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.userId = :userId ORDER BY uc.issuedAt DESC")
    fun findAllByUserId(userId: Long): List<UserCouponEntity>

    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.couponId = :couponId ORDER BY uc.issuedAt DESC")
    fun findAllByCouponId(couponId: Long): List<UserCouponEntity>
}
