package com.loopers.batch.job.stock

data class ProductStock(
    val productId: Long,
    val dbStock: Long,
)
