package com.loopers.application.user.product

import org.springframework.context.ApplicationEvent

class ProductDetailViewedEvent(
    val productId: Long,
) : ApplicationEvent(productId)
