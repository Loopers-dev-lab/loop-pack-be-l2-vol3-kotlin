package com.loopers.domain.brand

import com.loopers.domain.product.ProductReader
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class BrandRemover(
    private val brandReader: BrandReader,
    private val brandRepository: BrandRepository,
    private val productReader: ProductReader,
) {

    fun remove(id: Long) {
        val brand = brandReader.getById(id)

        if (productReader.existsSellingByBrandId(id)) {
            throw CoreException(ErrorType.BRAND_HAS_ACTIVE_PRODUCTS)
        }

        brand.deactivate()
        brandRepository.save(brand)
    }
}
