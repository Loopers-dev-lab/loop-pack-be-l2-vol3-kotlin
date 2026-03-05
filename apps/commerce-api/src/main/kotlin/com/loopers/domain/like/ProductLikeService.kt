package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun like(command: LikeProductCommand) {
        val like = ProductLikeModel(userId = command.userId, productId = command.productId)
        try {
            productLikeRepository.save(like)
            productLikeRepository.flush()
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다.")
        }
    }

    @Transactional
    fun unlike(command: UnlikeProductCommand) {
        val like = productLikeRepository.findByUserIdAndProductId(command.userId, command.productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "좋아요를 찾을 수 없습니다.")
        productLikeRepository.delete(like)
    }

    @Transactional(readOnly = true)
    fun getLikedProducts(userId: Long, pageable: Pageable): Slice<ProductLikeInfo> {
        return productLikeRepository.findAllByUserId(userId, pageable)
            .map { ProductLikeInfo.from(it) }
    }
}

data class LikeProductCommand(
    val userId: Long,
    val productId: Long,
)

data class UnlikeProductCommand(
    val userId: Long,
    val productId: Long,
)
