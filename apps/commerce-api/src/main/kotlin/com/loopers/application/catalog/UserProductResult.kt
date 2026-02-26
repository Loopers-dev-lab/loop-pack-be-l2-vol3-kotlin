package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.domain.catalog.ProductInfo
import java.math.BigDecimal

data class UserGetProductResult(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val price: BigDecimal,
) {
    companion object {
        fun from(info: ProductInfo, brandName: String): UserGetProductResult {
            return UserGetProductResult(
                id = info.id,
                brandId = info.brandId,
                brandName = brandName,
                name = info.name,
                price = info.price,
            )
        }
    }
}

data class UserListProductsResult(
    val content: List<UserGetProductResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<UserGetProductResult>): UserListProductsResult {
            return UserListProductsResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
