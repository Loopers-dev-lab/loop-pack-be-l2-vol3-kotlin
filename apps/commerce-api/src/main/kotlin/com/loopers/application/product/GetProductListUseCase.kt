package com.loopers.application.product

import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSortType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetProductListUseCase(
    private val productRepository: ProductRepository,
) {
    fun getAllActive(brandId: Long?, sortType: ProductSortType): List<ProductInfo> {
        return productRepository.findAllActive(brandId, sortType).map { ProductInfo.from(it) }
    }

    fun getAll(brandId: Long?): List<ProductInfo> {
        return productRepository.findAll(brandId).map { ProductInfo.from(it) }
    }
}
