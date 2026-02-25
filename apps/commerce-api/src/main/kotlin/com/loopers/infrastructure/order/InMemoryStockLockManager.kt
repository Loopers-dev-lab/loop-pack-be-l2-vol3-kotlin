package com.loopers.infrastructure.order

import com.loopers.domain.order.StockLockManager
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class InMemoryStockLockManager : StockLockManager {

    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    override fun acquireLocksForTransaction(productIds: List<Long>) {
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
