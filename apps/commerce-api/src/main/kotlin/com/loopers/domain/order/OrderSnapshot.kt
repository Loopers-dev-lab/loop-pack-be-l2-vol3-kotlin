package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

data class OrderSnapshot(
    val productId: Long,
    val productName: String,
    val brandId: Long,
    val brandName: String,
    val regularPrice: Money,
    val sellingPrice: Money,
    val thumbnailUrl: String?,
) {
    init {
        if (productName.isBlank()) {
            throw CoreException(ErrorType.PRODUCT_INVALID_NAME)
        }
        if (brandName.isBlank()) {
            throw CoreException(ErrorType.BRAND_INVALID_NAME)
        }
        if (sellingPrice.isGreaterThan(regularPrice)) {
            throw CoreException(ErrorType.PRODUCT_INVALID_PRICE)
        }
    }
}
