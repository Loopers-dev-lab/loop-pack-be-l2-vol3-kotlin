package com.loopers.config.redis

object RedisKeys {
    private const val STOCK_KEY_PREFIX = "stock"
    private const val COUPON_USE_KEY_PREFIX = "coupon-use"

    fun stockKey(productId: Long) = "$STOCK_KEY_PREFIX:$productId"
    fun couponUseKey(couponId: Long, userId: Long) = "$COUPON_USE_KEY_PREFIX:$couponId:user:$userId"
}
