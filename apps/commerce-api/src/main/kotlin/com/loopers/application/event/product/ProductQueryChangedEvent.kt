package com.loopers.application.event.product

data class ProductQueryChangedEvent(
    val productIds: Collection<Long> = emptyList(),
    val brandIds: Collection<Long> = emptyList(),
)
