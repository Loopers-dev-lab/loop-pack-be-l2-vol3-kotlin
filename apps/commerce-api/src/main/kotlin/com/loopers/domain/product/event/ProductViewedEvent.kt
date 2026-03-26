package com.loopers.domain.product.event

import org.springframework.context.ApplicationEvent
import java.util.UUID

class ProductViewedEvent(
    source: Any,
    val productId: Long,
    val userId: Long,
    val dedupeKey: String = "product.viewed:$userId:$productId:${UUID.randomUUID()}",
) : ApplicationEvent(source)
