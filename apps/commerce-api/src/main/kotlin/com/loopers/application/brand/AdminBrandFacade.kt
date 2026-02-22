package com.loopers.application.brand

import com.loopers.domain.brand.BrandCommand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class AdminBrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    fun createBrand(command: BrandCommand.Create): BrandInfo {
        val brand = brandService.createBrand(command)
        return BrandInfo.from(brand)
    }

    fun getBrand(id: Long): BrandInfo {
        val brand = brandService.getBrandForAdmin(id)
        return BrandInfo.from(brand)
    }

    fun getBrands(page: Int, size: Int): Page<BrandInfo> {
        return brandService.getBrands(page, size).map { BrandInfo.from(it) }
    }

    fun updateBrand(id: Long, command: BrandCommand.Update): BrandInfo {
        val brand = brandService.updateBrand(id, command)
        return BrandInfo.from(brand)
    }

    fun deleteBrand(id: Long) {
        productService.deleteProductsByBrandId(id)
        brandService.deleteBrand(id)
    }
}
