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
        val likePage = likeRepository.findActiveLikesByUserId(userId, page, size)

        if (likePage.content.isEmpty()) {
            return PageResult.of(content = emptyList(), page = page, size = size, totalElements = likePage.totalElements)
        }

        val productIds = likePage.content.map { it.productId }
        val productMap = productRepository.findAllActiveByIds(productIds).associateBy { it.id }

        val likeProductInfos = likePage.content
            .filter { productMap.containsKey(it.productId) }
            .map { like ->
                LikeProductInfo.from(
                    product = productMap.getValue(like.productId),
                    likedAt = like.createdAt,
                )
            }

        return PageResult.of(
            content = likeProductInfos,
            page = page,
            size = size,
            totalElements = likePage.totalElements,
        )
    }
}
