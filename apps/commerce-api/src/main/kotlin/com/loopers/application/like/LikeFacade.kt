package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val userService: UserService,
    private val productService: ProductService,
) {
    @Transactional
    fun like(loginId: String, productId: Long): LikeInfo {
        val user = userService.getUserByLoginId(loginId)
        val (isNew, likeModel) = likeService.like(user.id, productId)
        if (isNew) productService.incrementLikeCount(productId)
        return LikeInfo.from(likeModel)
    }

    @Transactional
    fun unlike(loginId: String, productId: Long) {
        val user = userService.getUserByLoginId(loginId)
        val deleted = likeService.unlike(user.id, productId)
        if (deleted) productService.decrementLikeCount(productId)
    }

    fun getLikedProducts(loginId: String, userId: Long, pageable: Pageable): List<LikeInfo> {
        val user = userService.getUserByLoginId(loginId)
        if (user.id != userId) {
            throw CoreException(ErrorType.FORBIDDEN)
        }
        return likeService.getLikedProducts(user.id, pageable)
            .map { LikeInfo.from(it) }
    }
}
