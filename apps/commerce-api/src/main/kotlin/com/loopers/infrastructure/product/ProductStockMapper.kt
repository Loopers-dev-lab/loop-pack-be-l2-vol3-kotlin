package com.loopers.infrastructure.product

import com.loopers.domain.common.Quantity
import com.loopers.domain.product.ProductStock
import org.springframework.stereotype.Component

@Component
class ProductStockMapper {
    fun toDomain(entity: ProductStockEntity): ProductStock {
        return ProductStock.retrieve(
            id = entity.id!!,
            productId = entity.productId,
            quantity = Quantity(entity.quantity),
        )
    }

    fun toEntity(stock: ProductStock, admin: String): ProductStockEntity {
        return ProductStockEntity(
            id = stock.id,
            productId = stock.productId,
            quantity = stock.quantity.value,
            createdBy = admin,
            updatedBy = admin,
        )
    }
}
