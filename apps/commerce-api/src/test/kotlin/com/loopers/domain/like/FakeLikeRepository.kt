package com.loopers.domain.like

import com.loopers.domain.like.entity.Like
import com.loopers.domain.like.repository.LikeRepository

class FakeLikeRepository : LikeRepository {

    private val likes = mutableListOf<Like>()
    private var sequence = 1L

    override fun save(like: Like): Like {
        if (like.id != 0L) {
            likes.removeIf { it.id == like.id }
            likes.add(like)
        } else {
            setId(like, sequence++)
            likes.add(like)
        }
        return like
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return likes.find { it.refUserId == userId && it.refProductId == productId }
    }

    override fun delete(like: Like) {
        likes.removeIf { it.id == like.id }
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return likes.filter { it.refUserId == userId }.sortedByDescending { it.id }
    }

    private fun setId(entity: Like, id: Long) {
        Like::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
