package com.loopers.application.api.productlike

import com.loopers.domain.product.ProductService
import com.loopers.domain.productlike.ProductLikeService
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.user.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeFacade(
    private val productLikeService: ProductLikeService,
    private val productService: ProductService,
    private val userService: UserService,
) {

    @Transactional
    fun likeProduct(userId: Long, productId: Long) {
        val user = userService.getUser(userId)
        val product = productService.getProduct(productId)
        productLikeService.addProductLike(user, product)
    }

    @Transactional
    fun unlikeProduct(userId: Long, productId: Long) {
        val user = userService.getUser(userId)
        val product = productService.getProduct(productId)
        productLikeService.removeProductLike(user, product)
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeService.getMyLikedProducts(userId, pageable)
}
