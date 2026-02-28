package com.loopers.domain.like.fixture

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import java.time.ZonedDateTime

class FakeLikeRepository : LikeRepository {

    private val store = mutableSetOf<Pair<Long, Long>>()
    private var sequence = 1L

    override fun addLike(userId: Long, productId: Long): Boolean {
        return store.add(Pair(userId, productId))
    }

    override fun removeLike(userId: Long, productId: Long): Int {
        return if (store.remove(Pair(userId, productId))) 1 else 0
    }

    override fun findAllByUserId(userId: Long, page: Int, size: Int): List<Like> {
        return store.filter { it.first == userId }
            .drop(page * size)
            .take(size)
            .map { (uid, pid) ->
                Like.reconstitute(
                    persistenceId = sequence++,
                    refUserId = uid,
                    refProductId = pid,
                    createdAt = ZonedDateTime.now(),
                )
            }
    }

    override fun countByUserId(userId: Long): Long {
        return store.count { it.first == userId }.toLong()
    }
}
