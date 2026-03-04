package com.loopers.application.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeListUseCase(
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productStockRepository: ProductStockRepository,
) {
    @Transactional(readOnly = true)
    fun getList(userId: Long, pageRequest: PageRequest): PageResponse<UserProductLikeResult.LikedProduct> {
        val likePage = productLikeRepository.findAllByUserId(userId, pageRequest)
        val productIds = likePage.content.map { it.productId }

        if (productIds.isEmpty()) {
            return PageResponse(emptyList(), likePage.totalElements, likePage.page, likePage.size)
        }

        val products = productRepository.findAllByIdIn(productIds)
            .filter { it.status == Product.Status.ACTIVE }
            .associateBy { it.id!! }

        val brandIds = products.values.map { it.brandId }.distinct()
        val activeBrands = brandRepository.findAllByIdIn(brandIds)
            .filter { it.status == Brand.Status.ACTIVE }
            .associateBy { it.id!! }

        val stocks = productStockRepository.findAllByProductIdIn(productIds)
            .associateBy { it.productId }

        val likedProducts = likePage.content
            .filter { products.containsKey(it.productId) }
            .filter { activeBrands.containsKey(products[it.productId]!!.brandId) }
            .map { like ->
                val product = products[like.productId]!!
                val brand = activeBrands[product.brandId]!!
                val stock = stocks[product.id!!]
                UserProductLikeResult.LikedProduct.from(product, brand, stock)
            }

        return PageResponse(likedProducts, likePage.totalElements, likePage.page, likePage.size)
    }
}
