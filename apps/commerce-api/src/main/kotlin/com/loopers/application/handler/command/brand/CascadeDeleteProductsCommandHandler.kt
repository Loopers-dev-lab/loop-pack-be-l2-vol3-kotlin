package com.loopers.application.handler.command.brand

import com.loopers.application.product.ProductService
import com.loopers.domain.common.command.CascadeDeleteProductsCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CascadeDeleteProductsCommandHandler(
    private val productService: ProductService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: CascadeDeleteProductsCommand) {
        productService.deleteProductsByBrandId(command.brandId)
    }
}
