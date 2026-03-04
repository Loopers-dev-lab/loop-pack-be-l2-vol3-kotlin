package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserCouponJpaRepository : JpaRepository<UserCouponEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.id = :id AND uc.deletedAt IS NULL")
    fun findByIdWithLock(@Param("id") id: Long): Optional<UserCouponEntity>

    @Query("SELECT uc FROM UserCouponEntity uc WHERE uc.userId = :userId AND uc.deletedAt IS NULL")
    fun findAllByUserId(@Param("userId") userId: Long): List<UserCouponEntity>

    fun existsByUserIdAndCouponTemplateId(userId: Long, couponTemplateId: Long): Boolean
}
