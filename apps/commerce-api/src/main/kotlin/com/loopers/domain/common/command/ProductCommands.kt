package com.loopers.domain.common.command

data class DeductStockCommand(
    val productId: Long,
    val quantity: Int,
)

data class RestoreStockCommand(
    val productId: Long,
    val quantity: Int,
)

data class CascadeDeleteProductsCommand(
    val brandId: Long,
)
