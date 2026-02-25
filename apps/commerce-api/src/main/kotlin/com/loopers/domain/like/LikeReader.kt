package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class LikeReader(
    private val likeRepository: LikeRepository,
) {

    fun getById(id: Long): Like {
        return likeRepository.findById(id)
            ?: throw CoreException(ErrorType.LIKE_NOT_FOUND)
    }

    fun getAllByMemberId(memberId: Long): List<Like> {
        return likeRepository.findAllByMemberId(memberId)
    }

    fun countByProductId(productId: Long): Long {
        return likeRepository.countByProductId(productId)
    }

    fun countByProductIds(productIds: List<Long>): Map<Long, Long> {
        if (productIds.isEmpty()) return emptyMap()
        return likeRepository.countByProductIds(productIds)
    }
}
