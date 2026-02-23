package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductInventoryModel
import org.springframework.data.jpa.repository.JpaRepository

interface ProductInventoryJpaRepository : JpaRepository<ProductInventoryModel, Long> {
    fun findByProductId(productId: Long): ProductInventoryModel?
}
