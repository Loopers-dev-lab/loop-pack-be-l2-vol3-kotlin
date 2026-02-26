package com.loopers.domain.brand

import com.loopers.domain.product.Product

class BrandDomainService {

    fun deleteBrand(brand: Brand, products: List<Product>) {
        brand.delete()
        products.forEach { it.delete() }
    }
}
