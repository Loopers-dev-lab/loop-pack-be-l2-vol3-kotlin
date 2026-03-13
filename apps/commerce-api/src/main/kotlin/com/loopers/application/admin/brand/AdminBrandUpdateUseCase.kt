package com.loopers.application.admin.brand

import com.loopers.application.product.ProductQueryCache
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandUpdateUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productQueryCache: ProductQueryCache,
) {
    @Transactional
    fun update(command: AdminBrandCommand.Update): AdminBrandResult.Update {
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        val updated = brand.update(command.name, command.status)
        val saved = brandRepository.save(updated, command.admin)
        val productIds = productRepository.findAllByBrandId(saved.id!!)
            .mapNotNull { it.id }
        productQueryCache.evictDetails(productIds)
        return AdminBrandResult.Update.from(saved)
    }
}
