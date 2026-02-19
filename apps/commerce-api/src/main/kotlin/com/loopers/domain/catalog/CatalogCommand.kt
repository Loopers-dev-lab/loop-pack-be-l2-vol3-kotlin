package com.loopers.domain.catalog

import com.loopers.domain.catalog.product.entity.Product
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
    ) : CatalogCommand
}
