package com.loopers.application.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.UpdateBrandCommand
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {

    @Transactional(readOnly = true)
    fun getBrand(brandId: Long): Brand {
        return brandService.getBrand(brandId)
    }

    @Transactional(readOnly = true)
    fun getAllBrands(pageable: Pageable): Page<Brand> {
        return brandService.getAllBrands(pageable)
    }

    @Transactional
    fun createBrand(command: CreateBrandCommand): Brand {
        return brandService.createBrand(command)
    }

    @Transactional
    fun updateBrand(brandId: Long, command: UpdateBrandCommand): Brand {
        return brandService.updateBrand(brandId, command)
    }

    @Transactional
    fun deleteBrand(brandId: Long) {
        brandService.deleteBrand(brandId)
        productService.deleteProductsByBrandId(brandId)
    }
}
