package com.loopers.application.order

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class StockLockManager {

    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    /**
     * 상품 ID 목록에 대해 락을 획득하고, 트랜잭션 완료 후 자동으로 해제한다.
     * 데드락 방지를 위해 productId 오름차순으로 락을 획득한다.
     */
    fun acquireLocksForTransaction(productIds: List<Long>) {
        val sortedIds = productIds.sorted()
        val acquiredLocks = mutableListOf<ReentrantLock>()

        for (productId in sortedIds) {
            val lock = locks.computeIfAbsent(productId) { ReentrantLock() }
            lock.lock()
            acquiredLocks.add(lock)
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                acquiredLocks.forEach { it.unlock() }
            }
        })
    }
}
