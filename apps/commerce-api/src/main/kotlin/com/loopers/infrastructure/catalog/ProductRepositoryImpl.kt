package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductModel
import com.loopers.domain.catalog.ProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {
    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAll(pageable: Pageable): Slice<ProductModel> {
        return productJpaRepository.findAllByDeletedAtIsNull(pageable)
    }

    override fun findAllByBrandId(brandId: Long): List<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
    }

    override fun findAllByBrandId(brandId: Long, pageable: Pageable): Slice<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
    }

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun update(product: ProductModel) {
        productJpaRepository.save(product)
    }
}
