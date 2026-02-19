package com.loopers.application.like

import com.loopers.application.common.PageResult
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyLikesUseCase(
    private val likeRepository: LikeRepository,
) {
    fun getMyLikes(userId: Long, page: Int, size: Int): PageResult<LikeInfo> {
        val likes = likeRepository.findAllByUserId(userId, page, size)
        val totalElements = likeRepository.countByUserId(userId)
        val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
        return PageResult(
            content = likes.map { LikeInfo.from(it) },
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }
}
