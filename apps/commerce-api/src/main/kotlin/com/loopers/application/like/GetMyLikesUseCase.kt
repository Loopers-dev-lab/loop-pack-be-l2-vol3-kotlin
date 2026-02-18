package com.loopers.application.like

import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyLikesUseCase(
    private val likeRepository: LikeRepository,
) {
    fun getMyLikes(userId: Long): List<LikeInfo> {
        return likeRepository.findAllByUserId(userId).map { LikeInfo.from(it) }
    }
}
