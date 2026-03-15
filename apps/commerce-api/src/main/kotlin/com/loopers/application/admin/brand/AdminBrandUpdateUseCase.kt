package com.loopers.application.admin.brand

import com.loopers.application.event.product.ProductQueryChangedEvent
import com.loopers.application.event.product.ProductQueryChangedEventPublisher
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
    private val productQueryChangedEventPublisher: ProductQueryChangedEventPublisher,
) {
    @Transactional
    fun update(command: AdminBrandCommand.Update): AdminBrandResult.Update {
        val brand = brandRepository.findById(command.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        val updated = brand.update(command.name, command.status)
        val saved = brandRepository.save(updated, command.admin)
        val productIds = productRepository.findAllByBrandId(command.brandId)
            .mapNotNull { it.id }
        productQueryChangedEventPublisher.publish(
            ProductQueryChangedEvent(
                productIds = productIds,
                brandIds = listOf(command.brandId),
            ),
        )
        return AdminBrandResult.Update.from(saved)
    }
}
