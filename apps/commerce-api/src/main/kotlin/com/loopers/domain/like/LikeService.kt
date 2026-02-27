package com.loopers.domain.like

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {
    /**
     * 좋아요를 등록한다. 멱등성을 보장한다.
     * - 기록 없음 -> 새로 생성
     * - 삭제된 기록 있음 -> restore()로 복원
     * - 활성 기록 있음 -> 아무 작업 없이 반환
     *
     * @return 신규 등록 여부 (true: 신규 등록 또는 복원, false: 이미 활성 상태)
     */
    @Transactional
    fun like(userId: Long, productId: Long): Boolean {
        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)

        if (existingLike == null) {
            val newLike = LikeModel(userId = userId, productId = productId)
            likeRepository.save(newLike)
            return true
        }

        if (existingLike.deletedAt != null) {
            existingLike.restore()
            likeRepository.save(existingLike)
            return true
        }

        return false
    }

    /**
     * 좋아요를 취소한다. 멱등성을 보장한다.
     * - 활성 기록 있음 -> delete()로 소프트 삭제
     * - 기록 없음 또는 이미 삭제됨 -> 아무 작업 없이 반환
     *
     * @return 실제로 삭제가 수행되었는지 여부 (true: 활성 -> 삭제, false: 이미 삭제 또는 기록 없음)
     */
    @Transactional
    fun unlike(userId: Long, productId: Long): Boolean {
        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: return false

        if (existingLike.deletedAt != null) {
            return false
        }

        existingLike.delete()
        likeRepository.save(existingLike)
        return true
    }

    /** 유저의 활성 좋아요 목록을 조회한다. */
    @Transactional(readOnly = true)
    fun findByUserId(userId: Long, pageable: Pageable): Page<LikeModel> {
        return likeRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }
}
