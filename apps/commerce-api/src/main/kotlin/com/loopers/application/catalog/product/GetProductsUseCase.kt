package com.loopers.application.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.ProductSort
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.BrandId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductsUseCase(private val productRepository: ProductRepository) {
    @Transactional(readOnly = true)
    fun execute(brandId: Long?, sort: String, page: Int, size: Int): PageResult<ProductInfo> {
        val domainSort = ProductSort.entries.find { it.name == sort.uppercase() }
            ?: throw CoreException(
                ErrorType.BAD_REQUEST,
                "잘못된 정렬 기준입니다. 사용 가능한 값: ${ProductSort.entries.joinToString(", ")}",
            )
        return productRepository.findActiveProducts(
            brandId?.let { BrandId(it) },
            domainSort,
            page,
            size,
        ).map { ProductInfo.from(it) }
    }
}
