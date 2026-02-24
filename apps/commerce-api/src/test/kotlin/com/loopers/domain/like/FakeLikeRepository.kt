package com.loopers.domain.like

import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.dao.DataIntegrityViolationException

class FakeLikeRepository : LikeRepository {

    private val likes = mutableListOf<Like>()

    // 동시성 시나리오 재현용: find에는 보이지 않지만 save 시 unique constraint를 위반시키는 레코드
    private val hiddenLikes = mutableListOf<Like>()
    private var sequence = 1L

    override fun save(like: Like): Like {
        if (like.id != 0L) {
            likes.removeIf { it.id == like.id }
            likes.add(like)
        } else {
            // DB unique constraint(ref_user_id, ref_product_id) 재현:
            // likes 또는 hiddenLikes에 이미 같은 (userId, productId)가 있으면 예외
            val isDuplicate = (likes + hiddenLikes).any {
                it.refUserId == like.refUserId && it.refProductId == like.refProductId
            }
            if (isDuplicate) {
                throw DataIntegrityViolationException("Unique constraint violation: (ref_user_id, ref_product_id)")
            }
            setId(like, sequence++)
            likes.add(like)
        }
        return like
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        // hiddenLikes는 조회되지 않는다 (동시성 시나리오에서 체크 통과를 재현)
        return likes.find { it.refUserId == userId && it.refProductId == productId }
    }

    override fun delete(like: Like) {
        likes.removeIf { it.id == like.id }
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return likes.filter { it.refUserId == userId }.sortedByDescending { it.id }
    }

    /**
     * 동시성 시나리오 재현용:
     * findByUserIdAndProductId 체크에는 보이지 않지만(체크 통과),
     * save 시점에 이미 DB에 레코드가 존재하는 상황을 만든다.
     * 이후 save 호출 시 DataIntegrityViolationException이 발생한다.
     */
    fun simulateConcurrentInsert(userId: Long, productId: Long) {
        val like = Like(refUserId = userId, refProductId = productId)
        setId(like, sequence++)
        hiddenLikes.add(like)
    }

    private fun setId(entity: Like, id: Long) {
        Like::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
