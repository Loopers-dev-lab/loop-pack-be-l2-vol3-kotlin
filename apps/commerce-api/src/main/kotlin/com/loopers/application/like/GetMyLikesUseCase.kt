package com.loopers.application.like

import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetMyLikesUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, page: Int, size: Int): PageResult<LikeProductInfo> {
        val likes = likeRepository.findAllByUserId(userId)
        val productIds = likes.map { it.productId }

        val activeProducts = productRepository.findAllActiveByIds(productIds)
        val productMap = activeProducts.associateBy { it.id }

        val likeProductInfos = likes
            .filter { productMap.containsKey(it.productId) }
            .map { like ->
                LikeProductInfo.from(
                    product = productMap.getValue(like.productId),
                    likedAt = like.createdAt,
                )
            }

        val totalElements = likeProductInfos.size.toLong()
        val fromIndex = (page * size).coerceAtMost(likeProductInfos.size)
        val toIndex = ((page + 1) * size).coerceAtMost(likeProductInfos.size)
        val pagedContent = likeProductInfos.subList(fromIndex, toIndex)

        return PageResult.of(
            content = pagedContent,
            page = page,
            size = size,
            totalElements = totalElements,
        )
    }
}
