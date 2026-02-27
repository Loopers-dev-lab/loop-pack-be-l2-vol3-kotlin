package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    fun createBrand(name: String, description: String?): BrandInfo {
        return brandService.createBrand(name, description)
            .let { BrandInfo.from(it) }
    }

    fun getBrand(brandId: Long): BrandInfo {
        return brandService.getBrand(brandId)
            .let { BrandInfo.from(it) }
    }

    fun getBrands(pageable: Pageable): Page<BrandInfo> {
        return brandService.getBrands(pageable)
            .map { BrandInfo.from(it) }
    }

    fun updateBrand(brandId: Long, name: String, description: String?): BrandInfo {
        return brandService.updateBrand(brandId, name, description)
            .let { BrandInfo.from(it) }
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        val brand = brandService.getBrand(brandId)
        brandService.delete(brand)
        productService.deleteAllByBrandId(brandId)
    }
}
