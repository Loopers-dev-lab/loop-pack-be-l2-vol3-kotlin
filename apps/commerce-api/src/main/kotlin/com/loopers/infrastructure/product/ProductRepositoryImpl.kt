package com.loopers.infrastructure.product

import com.loopers.domain.common.CursorResult
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val productQueryDslRepository: ProductQueryDslRepository,
) : ProductRepository {
    override fun save(product: ProductModel): ProductModel {
        if (product.id == 0L) {
            return productJpaRepository.save(ProductJpaModel.from(product)).toModel()
        }
        val existing = productJpaRepository.findById(product.id).orElseThrow()
        existing.updateFrom(product)
        return existing.toModel()
    }

    override fun findById(id: Long): ProductModel? {
        return productJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findByIdWithLock(id: Long): ProductModel? {
        return productJpaRepository.findByIdWithLock(id)?.toModel()
    }

    override fun findAll(pageQuery: PageQuery): PageResult<ProductModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = productJpaRepository.findAll(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun findAllByBrandId(brandId: Long, pageQuery: PageQuery): PageResult<ProductModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = productJpaRepository.findAllByBrandId(brandId, pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    override fun findAllByIdIn(ids: List<Long>): List<ProductModel> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllByIdIn(ids).map { it.toModel() }
    }

    override fun findActiveProducts(condition: ProductSearchCondition): CursorResult<ProductModel> {
        return productQueryDslRepository.findActiveProducts(condition)
    }

    override fun findAllByBrandIdAndStatus(brandId: Long, status: ProductStatus): List<ProductModel> {
        return productJpaRepository.findAllByBrandIdAndStatus(brandId, status).map { it.toModel() }
    }
}
