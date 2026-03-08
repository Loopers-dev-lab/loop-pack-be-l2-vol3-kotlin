package com.loopers.domain.product

import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ProductDomainService {

    fun updateProductInfo(
        product: Product,
        name: String,
        price: BigDecimal,
        status: ProductStatus,
    ) {
        product.updateInfo(name, price)
        product.changeStatus(status)
    }
}
