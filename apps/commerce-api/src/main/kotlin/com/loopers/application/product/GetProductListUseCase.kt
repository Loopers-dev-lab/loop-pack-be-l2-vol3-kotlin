package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductListUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
) {

    @Transactional(readOnly = true)
    fun execute(command: ProductCommand.Search): PageResult<ProductInfo> {
        val condition = ProductSearchCondition(
            brandId = command.brandId,
            sort = command.sort,
            page = command.page,
            size = command.size,
            includeDeleted = command.includeDeleted,
        )

        val pageResult = productRepository.findAllByCondition(condition)

        val brandIds = pageResult.content.map { it.brandId }.distinct()
        val brandMap = if (brandIds.isNotEmpty()) {
            brandRepository.findAllByIds(brandIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val productIds = pageResult.content.map { it.id }
        val stockMap = if (productIds.isNotEmpty()) {
            productStockRepository.findAllByProductIds(productIds)
                .associateBy { it.productId }
        } else {
            emptyMap()
        }

        val productInfos = pageResult.content.map { product ->
            val brandName = brandMap[product.brandId]?.name ?: ""
            val stock = stockMap[product.id]?.stock?.quantity ?: 0
            ProductInfo.from(product, brandName, stock)
        }

        return PageResult.of(
            content = productInfos,
            page = pageResult.page,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
        )
    }
}
