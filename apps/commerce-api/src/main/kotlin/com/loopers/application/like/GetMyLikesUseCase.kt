package com.loopers.application.like

import com.loopers.application.common.PageResult
import com.loopers.domain.like.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyLikesUseCase(
    private val likeRepository: LikeRepository,
) {
    fun getMyLikes(requestUserId: Long, targetUserId: Long, page: Int, size: Int): PageResult<LikeInfo> {
        if (requestUserId != targetUserId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 좋아요 목록만 조회할 수 있습니다.")
        }
        val likes = likeRepository.findAllByUserId(targetUserId, page, size)
        val totalElements = likeRepository.countByUserId(targetUserId)
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
