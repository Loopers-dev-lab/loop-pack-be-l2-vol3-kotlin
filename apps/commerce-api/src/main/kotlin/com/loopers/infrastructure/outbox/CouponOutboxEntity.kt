package com.loopers.infrastructure.outbox

import com.loopers.domain.BaseEntity
import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "coupon_outbox",
    indexes = [Index(name = "idx_coupon_outbox_published", columnList = "published,id")],
)
class CouponOutboxEntity(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "coupon_id", nullable = false)
    val couponId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "published", nullable = false)
    var published: Boolean = false,
) : BaseEntity() {

    companion object {
        fun fromDomain(outbox: CouponOutbox): CouponOutboxEntity {
            return CouponOutboxEntity(
                eventId = outbox.eventId,
                eventType = outbox.eventType,
                couponId = outbox.couponId,
                userId = outbox.userId,
                published = outbox.published,
            ).withBaseFields(id = outbox.id)
        }
    }

    fun toDomain(): CouponOutbox = CouponOutbox(
        id = id,
        eventId = eventId,
        eventType = eventType,
        couponId = couponId,
        userId = userId,
        published = published,
    )
}
