package com.loopers.domain.like

import com.loopers.domain.BaseEntity
import java.time.ZonedDateTime

class FakeLikeRepository : LikeRepository {

    private val store = mutableListOf<Like>()
    private var idSequence = 1L

    override fun save(like: Like): Like {
        if (like.id == 0L) {
            setEntityId(like, idSequence++)
            setCreatedAt(like, ZonedDateTime.now())
        }
        store.add(like)
        return like
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return store.find { it.userId == userId && it.productId == productId }
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return store.filter { it.userId == userId }
    }

    override fun delete(like: Like) {
        store.removeIf { it.id == like.id }
    }

    override fun deleteAllByProductId(productId: Long) {
        store.removeIf { it.productId == productId }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun setCreatedAt(entity: BaseEntity, time: ZonedDateTime) {
        val field = BaseEntity::class.java.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(entity, time)
    }
}
