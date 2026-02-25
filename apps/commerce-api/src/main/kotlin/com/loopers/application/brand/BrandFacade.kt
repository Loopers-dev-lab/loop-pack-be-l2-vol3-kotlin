package com.loopers.application.brand

import com.loopers.domain.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.CreateBrandCommand
import com.loopers.domain.brand.UpdateBrandCommand
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class BrandFacade(
    private val brandService: BrandService,
) {

    fun getBrand(brandId: Long): BrandResult {
        return brandService.findById(brandId)
            .let { BrandInfo.from(it) }
            .let { BrandResult.from(it) }
    }

    fun getBrands(pageable: Pageable): Page<BrandResult> {
        return brandService.findAll(pageable)
            .map { BrandInfo.from(it) }
            .map { BrandResult.from(it) }
    }

    fun createBrand(criteria: CreateBrandCriteria): BrandResult {
        val command = CreateBrandCommand(
            name = criteria.name,
            description = criteria.description,
            imageUrl = criteria.imageUrl,
        )
        return brandService.createBrand(command)
            .let { BrandInfo.from(it) }
            .let { BrandResult.from(it) }
    }

    fun updateBrand(brandId: Long, criteria: UpdateBrandCriteria): BrandResult {
        val command = UpdateBrandCommand(
            name = criteria.name,
            description = criteria.description,
            imageUrl = criteria.imageUrl,
        )
        return brandService.updateBrand(brandId, command)
            .let { BrandInfo.from(it) }
            .let { BrandResult.from(it) }
    }

    fun deleteBrand(brandId: Long) {
        // TODO: Step 5에서 연쇄 삭제 연결 (Like → Product → Brand)
        brandService.deleteBrand(brandId)
    }
}
