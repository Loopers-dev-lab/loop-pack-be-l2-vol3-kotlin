package com.loopers.infrastructure.outbox

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.withBaseFields
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "catalog_outbox",
    indexes = [Index(name = "idx_catalog_outbox_published", columnList = "published,id")],
)
class CatalogOutboxEntity(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: String,
    @Column(name = "event_type", nullable = false)
    val eventType: String,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "user_id")
    val userId: Long?,
    @Column(name = "published", nullable = false)
    var published: Boolean = false,
) : BaseEntity() {

    companion object {
        fun fromDomain(outbox: CatalogOutbox): CatalogOutboxEntity {
            return CatalogOutboxEntity(
                eventId = outbox.eventId,
                eventType = outbox.eventType.name,
                productId = outbox.productId.value,
                userId = outbox.userId?.value,
                published = outbox.published,
            ).withBaseFields(id = outbox.id)
        }
    }

    fun toDomain(): CatalogOutbox = CatalogOutbox(
        id = id,
        eventId = eventId,
        eventType = CatalogOutbox.CatalogOutboxEventType.valueOf(eventType),
        productId = ProductId(productId),
        userId = userId?.let { UserId(it) },
        published = published,
    )
}
