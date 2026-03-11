package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findByIdForUpdate(ProductId(productId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (product.isDeleted() || !product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }

        if (likeRepository.findByUserIdAndProductIdForUpdate(UserId(userId), ProductId(productId)) != null) return

        likeRepository.save(Like(refUserId = UserId(userId), refProductId = ProductId(productId)))
        product.increaseLikeCount()
        productRepository.save(product)
    }
}
