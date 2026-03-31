package com.loopers.support.event.user

import org.springframework.context.ApplicationEvent

class ProductLikeRegisteredEvent(
    val userId: Long,
    val productId: Long,
) : ApplicationEvent(productId)
