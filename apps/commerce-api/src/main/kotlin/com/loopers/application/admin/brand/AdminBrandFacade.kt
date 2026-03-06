package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminBrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    fun getBrandInfo(brandId: Long): BrandInfo = brandService.getBrandInfo(brandId)

    fun getAllBrands(pageable: Pageable): Page<BrandInfo> = brandService.getAllBrands(pageable)

    @Transactional
    fun createBrand(name: String, description: String): Brand = brandService.createBrand(name, description)

    @Transactional
    fun updateBrand(brandId: Long, name: String, description: String) = brandService.updateBrand(brandId, name, description)

    @Transactional
    fun deleteBrand(brandId: Long) {
        productService.deleteProductsByBrand(brandId)
        brandService.deleteBrand(brandId)
    }
}
