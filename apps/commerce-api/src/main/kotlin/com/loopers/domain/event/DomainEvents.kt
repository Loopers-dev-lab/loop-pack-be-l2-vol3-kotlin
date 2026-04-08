package com.loopers.domain.event

interface DomainEvent

data class OrderCreatedEvent(
    val orderId: Long,
    val userId: Long,
    val totalPrice: Long,
    val productIds: List<Long>,
) : DomainEvent

data class PaymentCompletedEvent(
    val paymentId: Long,
    val orderId: Long,
    val userId: Long,
    val amount: Long,
) : DomainEvent

data class ProductLikedEvent(
    val userId: Long,
    val productId: Long,
) : DomainEvent

data class ProductUnlikedEvent(
    val userId: Long,
    val productId: Long,
) : DomainEvent
