package com.loopers.domain.catalog

import com.loopers.domain.catalog.brand.entity.Brand
import com.loopers.domain.catalog.product.entity.Product

data class ProductDetail(
    val product: Product,
    val brand: Brand,
)
