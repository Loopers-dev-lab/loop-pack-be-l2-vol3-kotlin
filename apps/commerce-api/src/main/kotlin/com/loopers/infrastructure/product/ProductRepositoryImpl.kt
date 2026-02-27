package com.loopers.infrastructure.product

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.SortOrder
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {

    override fun save(product: Product): Product {
        return productJpaRepository.save(product)
    }

    override fun findAll(brandId: Long?, pageQuery: PageQuery): PageResult<Product> {
        val pageable = toPageRequest(pageQuery)
        val page = if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
        } else {
            productJpaRepository.findAllByDeletedAtIsNull(pageable)
        }
        return PageResult(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
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

    private fun toPageRequest(pageQuery: PageQuery): PageRequest {
        val direction = when (pageQuery.sort.direction) {
            SortOrder.Direction.ASC -> Sort.Direction.ASC
            SortOrder.Direction.DESC -> Sort.Direction.DESC
        }
        return PageRequest.of(pageQuery.page, pageQuery.size, Sort.by(direction, pageQuery.sort.property))
    }
}
