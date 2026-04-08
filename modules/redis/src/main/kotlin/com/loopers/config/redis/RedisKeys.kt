package com.loopers.config.redis

object RedisKeys {
    private const val STOCK_KEY_PREFIX = "stock"
    private const val COUPON_USE_KEY_PREFIX = "coupon-use"
    private const val ORDER_QUEUE_KEY = "order-queue"
    private const val ENTRY_TOKEN_KEY_PREFIX = "entry-token"
    private const val RANKING_KEY_PREFIX = "ranking:all"

    fun stockKey(productId: Long) = "$STOCK_KEY_PREFIX:$productId"
    fun couponUseKey(couponId: Long, userId: Long) = "$COUPON_USE_KEY_PREFIX:$couponId:user:$userId"
    fun orderQueueKey() = ORDER_QUEUE_KEY
    fun entryTokenKey(userId: Long) = "$ENTRY_TOKEN_KEY_PREFIX:$userId"
    fun rankingKey(date: String) = "$RANKING_KEY_PREFIX:$date"
}
