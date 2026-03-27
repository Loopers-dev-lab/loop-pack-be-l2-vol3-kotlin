package com.loopers.application.user.like

import org.springframework.context.ApplicationEvent

class ProductLikeRegisteredEvent(
    val userId: Long,
    val productId: Long,
) : ApplicationEvent(productId)
