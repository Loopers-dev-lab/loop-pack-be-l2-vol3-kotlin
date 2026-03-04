package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class LikeRemover(
    private val likeReader: LikeReader,
    private val likeRepository: LikeRepository,
) {

    fun remove(likeId: Long, memberId: Long) {
        val like = likeReader.getById(likeId)

        if (like.memberId != memberId) {
            throw CoreException(ErrorType.LIKE_NOT_OWNER)
        }

        likeRepository.delete(like)
    }
}
