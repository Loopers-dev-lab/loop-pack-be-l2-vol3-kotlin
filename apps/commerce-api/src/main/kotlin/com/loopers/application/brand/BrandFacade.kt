package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    fun getBrand(id: Long): BrandInfo {
        return brandService.getBrand(id)
            .let { BrandInfo.from(it) }
    }

    fun getBrands(pageable: Pageable): Page<BrandInfo> {
        return brandService.getBrands(pageable)
            .map { BrandInfo.from(it) }
    }

    fun createBrand(name: String, description: String): BrandInfo {
        return brandService.createBrand(name, description)
            .let { BrandInfo.from(it) }
    }

    fun updateBrand(id: Long, name: String, description: String): BrandInfo {
        return brandService.updateBrand(id, name, description)
            .let { BrandInfo.from(it) }
    }

    @Transactional
    fun deleteBrand(id: Long) {
        brandService.deleteBrand(id)
        productService.softDeleteByBrandId(id)
    }
}
