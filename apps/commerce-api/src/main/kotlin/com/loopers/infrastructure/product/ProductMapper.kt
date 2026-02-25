package com.loopers.infrastructure.product

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import org.springframework.stereotype.Component

@Component
class ProductMapper {

    fun toDomain(entity: ProductEntity): Product {
        return Product(
            id = entity.id,
            brandId = entity.brandId,
            name = ProductName(entity.name),
            price = ProductPrice(entity.price),
            description = ProductDescription(entity.description),
            stock = Stock(entity.stock),
            status = ProductStatus.valueOf(entity.status),
        )
    }

    fun toEntity(domain: Product): ProductEntity {
        return ProductEntity(
            brandId = domain.brandId,
            name = domain.name.value,
            price = domain.price.value,
            description = domain.description.value,
            stock = domain.stock.value,
            status = domain.status.name,
        )
    }

    fun update(entity: ProductEntity, domain: Product) {
        entity.name = domain.name.value
        entity.price = domain.price.value
        entity.description = domain.description.value
        entity.stock = domain.stock.value
        entity.status = domain.status.name
    }
}
