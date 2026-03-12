package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductSearchCondition
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class GetProductListUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
    private val productCacheStore: ProductCacheStore,
    private val transactionTemplate: TransactionTemplate,
) {

    fun execute(command: ProductCommand.Search): PageResult<ProductInfo> {
        if (command.includeDeleted || !isCacheable(command.page)) {
            return queryFromDbWithTx(command)
        }

        return queryWithCache(command)
    }

    private fun queryWithCache(command: ProductCommand.Search): PageResult<ProductInfo> {
        val version = productCacheStore.getListVersion()

        val listCache = productCacheStore.getListIds(
            version = version,
            brandId = command.brandId,
            sort = command.sort,
            page = command.page,
        )

        if (listCache != null) {
            val cachedDetails = productCacheStore.getDetails(listCache.productIds)
            val missingIds = listCache.productIds.filter { it !in cachedDetails }

            val details = if (missingIds.isEmpty()) {
                cachedDetails
            } else {
                val missingInfos = loadAndCacheDetailsWithTx(missingIds)
                cachedDetails + missingInfos
            }

            val orderedInfos = listCache.productIds.mapNotNull { details[it] }

            if (orderedInfos.size < listCache.productIds.size) {
                return queryAndCacheFromDb(command, version)
            }

            return PageResult.of(
                content = orderedInfos,
                page = command.page,
                size = command.size,
                totalElements = listCache.totalElements,
            )
        }

        return queryAndCacheFromDb(command, version)
    }

    private fun queryAndCacheFromDb(command: ProductCommand.Search, version: Long): PageResult<ProductInfo> {
        val result = queryFromDbWithTx(command)

        productCacheStore.putListIds(
            version = version,
            brandId = command.brandId,
            sort = command.sort,
            page = command.page,
            cache = ProductListCache(
                productIds = result.content.map { it.id },
                totalElements = result.totalElements,
            ),
        )

        result.content.forEach { productInfo ->
            productCacheStore.putDetail(productInfo.id, productInfo)
        }

        return result
    }

    private fun queryFromDbWithTx(command: ProductCommand.Search): PageResult<ProductInfo> {
        return requireNotNull(transactionTemplate.execute { queryFromDb(command) })
    }

    private fun queryFromDb(command: ProductCommand.Search): PageResult<ProductInfo> {
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

    private fun loadAndCacheDetailsWithTx(productIds: List<Long>): Map<Long, ProductInfo> {
        return transactionTemplate.execute { loadAndCacheDetails(productIds) } ?: emptyMap()
    }

    private fun loadAndCacheDetails(productIds: List<Long>): Map<Long, ProductInfo> {
        val products = productRepository.findAllActiveByIds(productIds)
        if (products.isEmpty()) return emptyMap()

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        val stockMap = productStockRepository.findAllByProductIds(productIds)
            .associateBy { it.productId }

        return products.associate { product ->
            val brandName = brandMap[product.brandId]?.name ?: ""
            val stock = stockMap[product.id]?.stock?.quantity ?: 0
            val info = ProductInfo.from(product, brandName, stock)
            productCacheStore.putDetail(product.id, info)
            product.id to info
        }
    }

    private fun isCacheable(page: Int): Boolean {
        return page <= ProductCacheStore.MAX_CACHEABLE_PAGE
    }
}
