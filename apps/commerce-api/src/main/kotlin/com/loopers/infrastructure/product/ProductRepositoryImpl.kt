package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: ProductModel): ProductModel {
        return productJpaRepository.save(product)
    }

    override fun findByIdAndDeletedAtIsNull(id: Long): ProductModel? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByDeletedAtIsNull(brandId: Long?, pageable: Pageable): Page<ProductModel> {
        return if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
        } else {
            productJpaRepository.findAllByDeletedAtIsNull(pageable)
        }
    }

    override fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductModel> {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }
}
