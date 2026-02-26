package com.loopers.application.product

import java.math.BigDecimal

data class ReservedProduct(
    val productId: Long,
    val productName: String,
    val brandId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
)

data class FailedReservation(
    val productId: Long,
    val reason: String,
)

data class StockReservationResult(
    val reservedProducts: List<ReservedProduct>,
    val failedReservations: List<FailedReservation>,
)
