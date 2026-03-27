package com.loopers.application.handler.brand

import com.loopers.application.product.ProductService
import com.loopers.domain.common.command.CascadeDeleteProductsCommand
import org.springframework.stereotype.Component

@Component
class CascadeDeleteProductsCommandHandler(
    private val productService: ProductService,
) {
    fun handle(command: CascadeDeleteProductsCommand) {
        productService.deleteProductsByBrandId(command.brandId)
    }
}
