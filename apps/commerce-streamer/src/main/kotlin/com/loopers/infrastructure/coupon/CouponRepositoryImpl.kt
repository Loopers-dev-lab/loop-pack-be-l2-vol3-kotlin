package com.loopers.infrastructure.coupon

import com.loopers.domain.BaseEntity
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.repository.CouponRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(
    name = "coupons",
    indexes = [
        Index(name = "idx_coupons_expired_at_deleted_at", columnList = "expired_at, deleted_at"),
    ],
)
class CouponEntity(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "type", nullable = false)
    var type: String,
    @Column(name = "value", nullable = false)
    var value: Long,
    @Column(name = "max_discount", precision = 10, scale = 2)
    var maxDiscount: BigDecimal?,
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    var minOrderAmount: BigDecimal?,
    @Column(name = "total_quantity")
    var totalQuantity: Int?,
    @Column(name = "issued_count", nullable = false)
    var issuedCount: Int,
    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime,
) : BaseEntity() {

    fun toDomain(): Coupon = Coupon(
        id = id,
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        expiredAt = expiredAt,
        deletedAt = deletedAt,
    )
}

interface CouponJpaRepository : JpaRepository<CouponEntity, Long>

@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
) : CouponRepository {

    override fun findById(id: Long): Coupon? =
        couponJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(coupon: Coupon): Coupon {
        val entity = couponJpaRepository.findById(coupon.id).orElseThrow()
        entity.issuedCount = coupon.issuedCount
        return couponJpaRepository.save(entity).toDomain()
    }
}
