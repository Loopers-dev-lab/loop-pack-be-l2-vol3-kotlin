package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrandFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {

    /**
     * 브랜드 삭제
     * 브랜드 삭제 시 해당 브랜드에 속한 상품들도 함께 삭제되어야 합니다. (cascade delete)
     * @param brandId 삭제할 브랜드의 ID
     */
    @Transactional
    fun deleteBrand(brandId: Long) {
        brandService.getById(brandId) // 존재 확인
        productService.deleteAllByBrandId(brandId)
        brandService.delete(brandId)
    }
}
