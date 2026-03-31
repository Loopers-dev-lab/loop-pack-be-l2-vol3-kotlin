package com.loopers.application.handler.command.product

import com.loopers.application.product.ProductService
import com.loopers.domain.common.command.DeductStockCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class DeductStockCommandHandler(
    private val productService: ProductService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: DeductStockCommand) {
        productService.deductStock(command.productId, command.quantity)
    }
}
