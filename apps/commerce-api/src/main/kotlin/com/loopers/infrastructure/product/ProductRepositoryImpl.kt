package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findByIdOrNull(id)
    }

    override fun findByIds(ids: List<Long>): List<Product> {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun findByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findByBrandIdAndDeletedAtIsNull(brandId)
    }

    override fun findAll(): List<Product> {
        return productJpaRepository.findAll()
    }

    override fun findAllForUser(pageable: Pageable, brandId: Long?): Page<Product> {
        return if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndStatusAndDisplayYnAndDeletedAtIsNull(
                brandId,
                ProductStatus.ACTIVE,
                true,
                pageable,
            )
        } else {
            productJpaRepository.findAllByStatusAndDisplayYnAndDeletedAtIsNull(
                ProductStatus.ACTIVE,
                true,
                pageable,
            )
        }
    }

    override fun findAllForAdmin(pageable: Pageable, brandId: Long?): Page<Product> {
        return if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
        } else {
            productJpaRepository.findAllByDeletedAtIsNull(pageable)
        }
    }

    override fun decreaseStock(productId: Long, quantity: Int): Int {
        return productJpaRepository.decreaseStock(productId, quantity)
    }
}
