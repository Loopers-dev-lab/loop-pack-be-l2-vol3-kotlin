package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): ProductEntity?
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ProductEntity>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long, pageable: Pageable): Page<ProductEntity>
    fun findAllByStatusAndDeletedAtIsNull(status: Product.Status, pageable: Pageable): Page<ProductEntity>
    fun findAllByStatusAndBrandIdAndDeletedAtIsNull(
        status: Product.Status,
        brandId: Long,
        pageable: Pageable,
    ): Page<ProductEntity>
    fun findAllByBrandIdAndDeletedAtIsNull(brandId: Long): List<ProductEntity>
    fun findAllByIdInAndDeletedAtIsNull(ids: List<Long>): List<ProductEntity>
}
