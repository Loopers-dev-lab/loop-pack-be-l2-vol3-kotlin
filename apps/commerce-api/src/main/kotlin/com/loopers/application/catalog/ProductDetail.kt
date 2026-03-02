package com.loopers.application.catalog

import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.product.model.Product

data class ProductDetail(
    val product: Product,
    val brand: Brand,
)
