package com.loopers.infrastructure.outbox

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.UserId
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
                eventType = outbox.eventType.name,
                couponId = outbox.couponId.value,
                userId = outbox.userId.value,
                published = outbox.published,
            ).withBaseFields(id = outbox.id)
        }
    }

    fun toDomain(): CouponOutbox = CouponOutbox(
        id = id,
        eventId = eventId,
        eventType = CouponOutbox.CouponOutboxEventType.valueOf(eventType),
        couponId = CouponId(couponId),
        userId = UserId(userId),
        published = published,
    )
}
