package com.loopers.domain.catalog

import com.loopers.domain.catalog.brand.Brand
import com.loopers.domain.catalog.product.Product

data class ProductDetail(
    val product: Product,
    val brand: Brand,
)
