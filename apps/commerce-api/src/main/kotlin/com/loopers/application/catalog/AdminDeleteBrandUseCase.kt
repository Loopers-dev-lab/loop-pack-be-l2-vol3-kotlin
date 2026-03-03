package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminDeleteBrandUseCase(
    private val brandService: BrandService,
    private val productService: ProductService,
) : UseCase<Long, Unit> {

    @Transactional
    override fun execute(brandId: Long) {
        brandService.getBrand(brandId)
        productService.deleteAllByBrandId(brandId)
        brandService.delete(brandId)
    }
}
