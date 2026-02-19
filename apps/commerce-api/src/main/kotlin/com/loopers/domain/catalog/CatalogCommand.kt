package com.loopers.domain.catalog

import com.loopers.domain.catalog.product.entity.Product
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal

sealed interface CatalogCommand {

    data class CreateBrand(val name: String) : CatalogCommand

    data class UpdateBrand(val name: String) : CatalogCommand

    data class CreateProduct(
        val brandId: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int,
    ) : CatalogCommand

    data class UpdateProduct(
        val name: String?,
        val price: BigDecimal?,
        val stock: Int?,
        val status: Product.ProductStatus?,
    ) : CatalogCommand {
        init {
            if (name == null && price == null && stock == null && status == null) {
                throw CoreException(ErrorType.BAD_REQUEST, "수정할 항목이 최소 1개 이상 필요합니다.")
            }
        }
    }
}
