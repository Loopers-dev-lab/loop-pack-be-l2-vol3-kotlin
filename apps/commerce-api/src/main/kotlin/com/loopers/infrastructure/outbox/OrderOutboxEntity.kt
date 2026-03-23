package com.loopers.infrastructure.outbox

import com.loopers.domain.BaseEntity
import com.loopers.domain.outbox.model.OrderOutbox
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "order_outbox",
    indexes = [Index(name = "idx_order_outbox_published", columnList = "published,id")],
)
class OrderOutboxEntity(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "total_amount")
    val totalAmount: Long?,
    @Column(name = "reason")
    val reason: String?,
    @Column(name = "product_id")
    val productId: Long?,
    @Column(name = "quantity")
    val quantity: Int?,
    @Column(name = "published", nullable = false)
    var published: Boolean = false,
) : BaseEntity() {

    companion object {
        fun fromDomain(outbox: OrderOutbox): OrderOutboxEntity {
            return OrderOutboxEntity(
                eventId = outbox.eventId,
                eventType = outbox.eventType,
                orderId = outbox.orderId,
                userId = outbox.userId,
                totalAmount = outbox.totalAmount,
                reason = outbox.reason,
                productId = outbox.productId,
                quantity = outbox.quantity,
                published = outbox.published,
            ).withBaseFields(id = outbox.id)
        }
    }

    fun toDomain(): OrderOutbox = OrderOutbox(
        id = id,
        eventId = eventId,
        eventType = eventType,
        orderId = orderId,
        userId = userId,
        totalAmount = totalAmount,
        reason = reason,
        productId = productId,
        quantity = quantity,
        published = published,
    )
}
