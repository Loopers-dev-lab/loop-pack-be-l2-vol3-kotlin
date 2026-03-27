package com.loopers.infrastructure.coupon

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): CouponEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponEntity c where c.id = :id and c.deletedAt is null")
    fun findByIdForUpdate(@Param("id") id: Long): CouponEntity?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponEntity>
}
