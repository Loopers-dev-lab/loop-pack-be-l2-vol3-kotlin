package com.loopers.application.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.dto.BrandInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminBrandFacade(
    private val brandService: BrandService,
) {
    fun getBrandInfo(id: Long): BrandInfo = brandService.getBrandInfo(id)

    fun getAllBrands(pageable: Pageable): Page<BrandInfo> = brandService.getAllBrands(pageable)

    @Transactional
    fun createBrand(name: String, description: String): Brand = brandService.createBrand(name, description)

    @Transactional
    fun updateBrand(id: Long, name: String, description: String) = brandService.updateBrand(id, name, description)

    @Transactional
    fun deleteBrand(id: Long) = brandService.deleteBrand(id)
}
