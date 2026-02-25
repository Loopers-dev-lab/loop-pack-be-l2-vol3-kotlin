package com.loopers.domain.like

import com.loopers.domain.product.ProductReader
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class LikeRegister(
    private val likeRepository: LikeRepository,
    private val productReader: ProductReader,
) {

    fun register(memberId: Long, productId: Long): Like {
        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            throw CoreException(ErrorType.ALREADY_LIKED)
        }

        productReader.getSellingById(productId)

        val like = Like(memberId = memberId, productId = productId)
        return likeRepository.save(like)
    }
}
