package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
import com.loopers.support.cursor.CursorUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional(readOnly = true)
    fun getProduct(productId: Long): ProductInfo {
        val product = productService.getProduct(productId)
        val brand = brandService.getBrand(product.brandId)
        return ProductInfo.from(product, brand.name)
    }

    @Transactional(readOnly = true)
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
            ProductSort.LATEST -> mapOf("id" to last.id)
            ProductSort.PRICE_ASC -> mapOf("price" to last.price, "id" to last.id)
            ProductSort.LIKES_DESC -> mapOf("likeCount" to last.likeCount, "id" to last.id)
        }
    }
}

data class ProductListResult(
    val data: List<ProductInfo>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
