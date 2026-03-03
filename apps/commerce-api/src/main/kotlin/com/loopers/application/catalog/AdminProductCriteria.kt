package com.loopers.application.catalog

import java.math.BigDecimal

data class RegisterProductCriteria(
    val brandId: Long,
    val name: String,
    val quantity: Int,
    val price: BigDecimal,
)

data class ListProductsCriteria(
    val page: Int,
    val size: Int,
    val brandId: Long? = null,
)
