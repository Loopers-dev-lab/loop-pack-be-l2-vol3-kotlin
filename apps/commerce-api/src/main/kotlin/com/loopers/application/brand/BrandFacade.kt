package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductInventoryService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
    private val productInventoryService: ProductInventoryService,
) {
    fun createBrand(
        name: String,
        logoImageUrl: String,
        description: String,
        zipCode: String,
        roadAddress: String,
        detailAddress: String,
        email: String,
        phoneNumber: String,
        businessNumber: String,
    ): BrandInfo =
        brandService.createBrand(
            name = name,
            logoImageUrl = logoImageUrl,
            description = description,
            zipCode = zipCode,
            roadAddress = roadAddress,
            detailAddress = detailAddress,
            email = email,
            phoneNumber = phoneNumber,
            businessNumber = businessNumber,
        ).let { BrandInfo.from(it) }

    fun getBrands(pageable: Pageable): Page<BrandInfo> =
        brandService.getBrands(pageable).map { BrandInfo.from(it) }

    fun getBrandById(id: Long): BrandInfo =
        brandService.getBrandById(id).let { BrandInfo.from(it) }

    fun updateBrand(
        id: Long,
        name: String,
        logoImageUrl: String,
        description: String,
        zipCode: String,
        roadAddress: String,
        detailAddress: String,
        email: String,
        phoneNumber: String,
        businessNumber: String,
    ): BrandInfo =
        brandService.updateBrand(
            id = id,
            name = name,
            logoImageUrl = logoImageUrl,
            description = description,
            zipCode = zipCode,
            roadAddress = roadAddress,
            detailAddress = detailAddress,
            email = email,
            phoneNumber = phoneNumber,
            businessNumber = businessNumber,
        ).let { BrandInfo.from(it) }

    @Transactional
    fun deleteBrand(id: Long) {
        brandService.deleteBrand(id)
        val productIds = productService.deleteAllByBrandId(id)
        productIds.forEach { productInventoryService.deleteInventory(it) }
    }
}
