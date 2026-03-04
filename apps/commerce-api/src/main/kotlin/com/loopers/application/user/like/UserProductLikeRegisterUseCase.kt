package com.loopers.application.user.like

import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeRegisterUseCase(
    private val productRepository: ProductRepository,
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun register(command: UserProductLikeCommand.Register) {
        val product = productRepository.findById(command.productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)

        if (product.status != Product.Status.ACTIVE) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }

        if (productLikeRepository.existsByUserIdAndProductId(command.userId, command.productId)) return

        val like = ProductLike.register(command.userId, command.productId)
        productLikeRepository.save(like)
    }
}
