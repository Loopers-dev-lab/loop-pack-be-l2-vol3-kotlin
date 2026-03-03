package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.Sort
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val productMapper: ProductMapper,
) : ProductRepository {

    override fun save(product: Product, admin: String): Product {
        val entity = if (product.id != null) {
            val existing = productJpaRepository.findById(product.id).orElseThrow()
            existing.name = product.name
            existing.regularPrice = product.regularPrice.amount
            existing.sellingPrice = product.sellingPrice.amount
            existing.imageUrl = product.imageUrl
            existing.thumbnailUrl = product.thumbnailUrl
            existing.likeCount = product.likeCount
            existing.status = product.status
            existing.updateBy(admin)
            existing
        } else {
            productMapper.toEntity(product, admin)
        }
        return productMapper.toDomain(productJpaRepository.saveAndFlush(entity))
    }

    override fun delete(productId: Long, admin: String) {
        val entity = productJpaRepository.findById(productId).orElseThrow()
        entity.deleteBy(admin)
        productJpaRepository.saveAndFlush(entity)
    }

    override fun findById(id: Long): Product? {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id)?.let { productMapper.toDomain(it) }
    }

    override fun findAll(pageRequest: PageRequest, brandId: Long?): PageResponse<Product> {
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size)
        val page = if (brandId != null) {
            productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)
        } else {
            productJpaRepository.findAllByDeletedAtIsNull(pageable)
        }
        return PageResponse(
            content = page.content.map { productMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun findAllActive(
        pageRequest: PageRequest,
        brandId: Long?,
        sort: Product.SortType?,
    ): PageResponse<Product> {
        val springSort = toSpringSort(sort)
        val pageable = SpringPageRequest.of(pageRequest.page, pageRequest.size, springSort)

        val page = if (brandId != null) {
            productJpaRepository.findAllByStatusAndBrandIdAndDeletedAtIsNull(
                Product.Status.ACTIVE,
                brandId,
                pageable,
            )
        } else {
            productJpaRepository.findAllByStatusAndDeletedAtIsNull(Product.Status.ACTIVE, pageable)
        }

        return PageResponse(
            content = page.content.map { productMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun findAllByBrandId(brandId: Long): List<Product> {
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
            .map { productMapper.toDomain(it) }
    }

    override fun findAllByIdIn(ids: List<Long>): List<Product> {
        if (ids.isEmpty()) return emptyList()
        return productJpaRepository.findAllByIdInAndDeletedAtIsNull(ids)
            .map { productMapper.toDomain(it) }
    }

    override fun deleteAllByBrandId(brandId: Long, admin: String) {
        val entities = productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId)
        entities.forEach { it.deleteBy(admin) }
        productJpaRepository.saveAllAndFlush(entities)
    }

    private fun toSpringSort(sortType: Product.SortType?): Sort {
        return when (sortType) {
            Product.SortType.PRICE_ASC -> Sort.by(Sort.Direction.ASC, "sellingPrice")
            Product.SortType.LIKES_DESC -> Sort.by(Sort.Direction.DESC, "likeCount")
            Product.SortType.LATEST, null -> Sort.by(Sort.Direction.DESC, "id")
        }
    }
}
