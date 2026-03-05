package com.loopers.application.admin.product

import com.loopers.domain.product.ProductRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminProductListUseCase(
    private val productRepository: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun getList(pageRequest: PageRequest, brandId: Long?): PageResponse<AdminProductResult.Summary> {
        return productRepository.findAll(pageRequest, brandId)
            .map { AdminProductResult.Summary.from(it) }
    }
}
