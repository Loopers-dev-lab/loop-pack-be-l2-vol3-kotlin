package com.loopers.domain.order

/**
 * Redis 기반 재고 선점 저장소.
 * 주문 시 DB 락 대신 Redis 원자 연산으로 재고를 선점/복원한다.
 */
interface StockReservationRepository {

    /**
     * 재고를 선점한다. (Redis DECR)
     * @return true: 선점 성공, false: 재고 부족 (자동 복원됨)
     */
    fun reserve(productId: Long, quantity: Int): Boolean

    /**
     * 선점한 재고를 복원한다. (Redis INCR)
     */
    fun restore(productId: Long, quantity: Int)

    /**
     * 상품 재고를 Redis에 설정한다. (캐시 워밍 용도)
     */
    fun setStock(productId: Long, quantity: Int)
}
