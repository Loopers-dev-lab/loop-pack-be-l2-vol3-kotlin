package com.loopers.application.handler.product

import com.loopers.application.product.ProductService
import com.loopers.domain.common.command.RestoreStockCommand
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RestoreStockCommandHandler(
    private val productService: ProductService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: RestoreStockCommand) {
        productService.restoreStock(command.productId, command.quantity)
    }
}
