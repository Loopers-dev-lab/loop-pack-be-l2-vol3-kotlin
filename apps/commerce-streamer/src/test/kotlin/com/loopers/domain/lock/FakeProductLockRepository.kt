package com.loopers.domain.lock

class FakeProductLockRepository : ProductLockRepository {

    private val existingIds = mutableSetOf<Long>()
    var callCount: Int = 0
        private set

    fun register(productId: Long) {
        existingIds.add(productId)
    }

    override fun findByIdForUpdate(id: Long): Long? {
        callCount++
        return if (id in existingIds) id else null
    }
}
