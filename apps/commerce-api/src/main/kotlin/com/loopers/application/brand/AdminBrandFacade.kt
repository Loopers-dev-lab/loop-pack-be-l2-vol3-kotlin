package com.loopers.application.brand

import com.loopers.application.product.ProductService
import com.loopers.domain.common.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    @Transactional
    fun createBrand(command: BrandCommand.Create): BrandInfo {
        val brand = brandService.createBrand(command)
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun getBrand(id: Long): BrandInfo {
        val brand = brandService.getBrandForAdmin(id)
        return BrandInfo.from(brand)
    }

    @Transactional(readOnly = true)
    fun getBrands(page: Int, size: Int): PageResult<BrandInfo> {
        val result = brandService.getBrands(page, size)
        return PageResult(
            content = result.content.map { BrandInfo.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    @Transactional
    fun updateBrand(id: Long, command: BrandCommand.Update): BrandInfo {
        val brand = brandService.updateBrand(id, command)
        return BrandInfo.from(brand)
    }

    @Transactional
    fun deleteBrand(id: Long) {
        productService.deleteProductsByBrandId(id)
        brandService.deleteBrand(id)
    }
}
