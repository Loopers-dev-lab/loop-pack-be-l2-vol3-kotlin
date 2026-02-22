package com.loopers.application.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductService
import com.loopers.support.cursor.CursorUtils
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun getProduct(productId: Long): ProductInfo {
        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    fun getProducts(condition: ProductSearchCondition): ProductListResult {
        val products = productService.getProducts(condition)
        val hasNext = products.size > condition.size
        val content = if (hasNext) products.dropLast(1) else products

        val brandIds = content.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { id ->
            runCatching { brandService.getBrand(id) }.getOrNull()?.name
        }

        val items = content.map { ProductInfo.from(it, brandMap[it.brandId]) }

        val nextCursor = if (hasNext && content.isNotEmpty()) {
            val last = content.last()
            val cursorMap = buildCursorMap(condition, last)
            CursorUtils.encode(cursorMap)
        } else {
            null
        }

        return ProductListResult(
            data = items,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    private fun buildCursorMap(
        condition: ProductSearchCondition,
        last: com.loopers.domain.product.ProductModel,
    ): Map<String, Any> {
        return when (condition.sort) {
            com.loopers.domain.product.ProductSort.LATEST -> mapOf("id" to last.id)
            com.loopers.domain.product.ProductSort.PRICE_ASC -> mapOf("price" to last.price, "id" to last.id)
            com.loopers.domain.product.ProductSort.LIKES_DESC -> mapOf("likeCount" to last.likeCount, "id" to last.id)
        }
    }
}

data class ProductListResult(
    val data: List<ProductInfo>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
