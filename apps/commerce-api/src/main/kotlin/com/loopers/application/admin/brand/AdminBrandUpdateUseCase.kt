package com.loopers.application.admin.brand

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.domain.product.ProductRepository
import com.loopers.support.transaction.AfterCommitExecutor
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminBrandUpdateUseCase(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val afterCommitExecutor: AfterCommitExecutor,
    private val productQueryInvalidator: ProductQueryInvalidator,
) {
    @Transactional
    fun update(command: AdminBrandCommand.Update): AdminBrandResult.Update {
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        val updated = brand.update(command.name, command.status)
        val saved = brandRepository.save(updated, command.admin)
        val productIds = productRepository.findAllByBrandId(command.brandId)
            .mapNotNull { it.id }
        afterCommitExecutor.execute {
            if (productIds.isNotEmpty()) {
                productQueryInvalidator.invalidateDetails(productIds)
            }
            productQueryInvalidator.invalidateListsByBrandId(command.brandId)
        }
        return AdminBrandResult.Update.from(saved)
    }
}
