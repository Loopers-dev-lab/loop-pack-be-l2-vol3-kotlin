package com.loopers.application.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class GetProductsUseCase(private val catalogService: CatalogService) {
    fun execute(brandId: Long?, sort: String, page: Int, size: Int): PageResult<ProductInfo> {
        val domainSort = ProductSort.entries.find { it.name == sort.uppercase() }
            ?: throw CoreException(
                ErrorType.BAD_REQUEST,
                "잘못된 정렬 기준입니다. 사용 가능한 값: ${ProductSort.entries.joinToString(", ")}",
            )
        return catalogService.getProducts(brandId, domainSort, page, size).map { ProductInfo.from(it) }
    }
}
