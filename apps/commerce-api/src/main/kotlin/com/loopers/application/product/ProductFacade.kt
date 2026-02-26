package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductSort
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
    fun getProducts(brandId: Long?, sort: String, size: Int, cursor: String?): ProductListResult {
        val productSort = resolveSort(sort)
        val condition = ProductSearchCondition(
            brandId = brandId,
            sort = productSort,
            size = size,
            cursor = cursor,
        )

        val cursorResult = productService.getProducts(condition)

        val brandIds = cursorResult.content.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { id ->
            runCatching { brandService.getBrand(id) }.getOrNull()?.name
        }

        val items = cursorResult.content.map { ProductInfo.from(it, brandMap[it.brandId]) }

        return ProductListResult(
            data = items,
            nextCursor = cursorResult.nextCursor,
            hasNext = cursorResult.hasNext,
        )
    }

    private fun resolveSort(sort: String): ProductSort {
        return when (sort.lowercase()) {
            "price_asc" -> ProductSort.PRICE_ASC
            "likes_desc" -> ProductSort.LIKES_DESC
            else -> ProductSort.LATEST
        }
    }
}

data class ProductListResult(
    val data: List<ProductInfo>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
