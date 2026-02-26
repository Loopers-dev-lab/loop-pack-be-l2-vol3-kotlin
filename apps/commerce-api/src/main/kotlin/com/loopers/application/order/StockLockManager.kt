package com.loopers.application.order

interface StockLockManager {

    /**
     * 상품 ID 목록에 대해 락을 획득하고, 트랜잭션 완료 후 자동으로 해제한다.
     * 데드락 방지를 위해 productId 오름차순으로 락을 획득한다.
     */
    fun acquireLocksForTransaction(productIds: List<Long>)
}
