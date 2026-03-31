package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class ProductDetailViewedEvent(
    val productId: Long,
) : ApplicationEvent(productId)
