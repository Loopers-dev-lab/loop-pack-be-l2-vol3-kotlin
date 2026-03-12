package com.loopers.application.product

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class GetProductUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
    private val productCacheStore: ProductCacheStore,
    transactionManager: PlatformTransactionManager,
) {

    private val readOnlyTx = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    fun execute(productId: Long): ProductInfo {
        val cached = productCacheStore.getDetail(productId)
        if (cached != null) return cached

        return requireNotNull(readOnlyTx.execute { loadFromDb(productId) })
    }

    private fun loadFromDb(productId: Long): ProductInfo {
        val product = productRepository.findActiveByIdOrNull(productId)
            ?: throw CoreException(ProductErrorCode.PRODUCT_NOT_FOUND)

        val brand = brandRepository.findActiveByIdOrNull(product.brandId)
        val brandName = brand?.name ?: ""

        val stock = productStockRepository.findByProductId(productId)?.stock?.quantity ?: 0

        val productInfo = ProductInfo.from(product, brandName, stock)
        productCacheStore.putDetail(productId, productInfo)
        return productInfo
    }
}
