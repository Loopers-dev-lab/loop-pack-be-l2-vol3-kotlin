package com.loopers.application.catalog

import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class CatalogCommand {

    data class CreateBrand(val name: BrandName)

    data class UpdateBrand(val name: BrandName)

    data class CreateProduct(
        val brandId: Long,
        val name: String,
        val price: Money,
        val stock: Int,
    )

    data class UpdateProduct(
        val name: String?,
        val price: Money?,
        val stock: Int?,
        val status: Product.ProductStatus?,
    ) {
        init {
            if (name == null && price == null && stock == null && status == null) {
                throw CoreException(ErrorType.BAD_REQUEST, "수정할 항목이 최소 1개 이상 필요합니다.")
            }
        }
    }
}
