package com.loopers.application.event

data class OrderCompletedEvent(
    val orderId: Long,
    val userId: Long,
    val totalAmount: Long,
    val orderItems: List<OrderCompletedItem> = emptyList(),
)

data class OrderCompletedItem(
    val productId: Long,
    val quantity: Int,
)

data class LikeChangedEvent(
    val userId: Long,
    val productId: Long,
    val actionType: LikeActionType,
)

enum class LikeActionType {
    LIKE,
    UNLIKE,
}

data class ProductViewedEvent(
    val productId: Long?,
    val actionType: ProductViewActionType,
)

enum class ProductViewActionType {
    PRODUCT_DETAIL_VIEWED,
    PRODUCT_LIST_VIEWED,
}

data class UserActionLogCommand(
    val userId: Long?,
    val actionType: UserActionType,
    val targetId: Long?,
    val metadata: Map<String, Any?> = emptyMap(),
)

enum class UserActionType {
    ORDER_CREATED,
    PRODUCT_LIKED,
    PRODUCT_UNLIKED,
    PRODUCT_DETAIL_VIEWED,
    PRODUCT_LIST_VIEWED,
}
