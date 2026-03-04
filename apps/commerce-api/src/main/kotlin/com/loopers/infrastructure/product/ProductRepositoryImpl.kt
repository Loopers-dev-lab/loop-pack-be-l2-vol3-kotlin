package com.loopers.infrastructure.product

import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.common.toPageRequest
import com.loopers.infrastructure.common.toPageResult
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findAll(brandId: Long?, pageQuery: PageQuery): PageResult<Product> {
        val pageable = pageQuery.toPageRequest()
        val page = if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
        } else {
            productJpaRepository.findAllByDeletedAtIsNull(pageable)
        }
        return page.toPageResult()
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)
    }

    override fun findAllByIds(ids: List<Long>): List<Product> {
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
    }
}
