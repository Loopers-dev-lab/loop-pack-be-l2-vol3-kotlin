package com.loopers.application.catalog

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal

class CatalogCommand {

    data class CreateBrand(val name: String)

    data class UpdateBrand(val name: String)

    data class CreateProduct(
        val brandId: Long,
        val name: String,
        val price: BigDecimal,
        val stock: Int,
    )

    data class UpdateProduct(
        val name: String?,
        val price: BigDecimal?,
        val stock: Int?,
        val status: String?,
    ) {
        init {
            if (name == null && price == null && stock == null && status == null) {
                throw CoreException(ErrorType.BAD_REQUEST, "수정할 항목이 최소 1개 이상 필요합니다.")
            }
        }
    }
}
