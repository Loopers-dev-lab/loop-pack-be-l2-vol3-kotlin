package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCoupon
import com.loopers.domain.coupon.UserCouponStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserCouponJpaRepository : JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.couponId = :couponId")
    fun findByUserIdAndCouponId(@Param("userId") userId: Long, @Param("couponId") couponId: Long): UserCoupon?

    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    @Query(
        "SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId" +
            " AND (:status IS NULL OR uc.status = :status)" +
            " ORDER BY uc.createdAt DESC",
    )
    fun findAllByUserId(
        @Param("userId") userId: Long,
        @Param("status") status: UserCouponStatus?,
        pageable: Pageable,
    ): Page<UserCoupon>

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.couponId = :couponId ORDER BY uc.createdAt DESC")
    fun findAllByCouponId(@Param("couponId") couponId: Long, pageable: Pageable): Page<UserCoupon>
}
