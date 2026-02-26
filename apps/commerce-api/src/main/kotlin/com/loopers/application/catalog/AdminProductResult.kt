package com.loopers.application.catalog

import com.loopers.application.SliceResult
import com.loopers.domain.catalog.ProductInfo
import java.math.BigDecimal

data class RegisterProductResult(
    val id: Long,
) {
    companion object {
        fun from(info: ProductInfo): RegisterProductResult {
            return RegisterProductResult(id = info.id)
        }
    }
}

data class GetProductResult(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun from(info: ProductInfo, brandName: String): GetProductResult {
            return GetProductResult(
                id = info.id,
                brandId = info.brandId,
                brandName = brandName,
                name = info.name,
                quantity = info.quantity,
                price = info.price,
            )
        }
    }
}

data class ListProductsResult(
    val content: List<GetProductResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<GetProductResult>): ListProductsResult {
            return ListProductsResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
