package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductDetailUseCase(
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(productId: Long): UserProductResult.Detail {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        if (product.status != Product.Status.ACTIVE) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }

        val brand = brandRepository.findById(product.brandId)
            ?: throw CoreException(ErrorType.BRAND_NOT_FOUND)
        if (brand.status != Brand.Status.ACTIVE) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }

        val stock = productStockRepository.findByProductId(productId)
            ?: throw CoreException(ErrorType.PRODUCT_STOCK_NOT_FOUND)

        return UserProductResult.Detail.from(product, brand, stock)
    }
}
