package com.loopers.domain.order

/**
 * Redis 기반 쿠폰 사용 선점 저장소.
 * 주문 시 DB 락 대신 Redis SETNX로 쿠폰 사용을 선점/복원한다.
 */
interface CouponReservationRepository {

    /**
     * 쿠폰 사용을 선점한다. (Redis SETNX)
     * @return true: 선점 성공, false: 이미 다른 주문에서 사용 중
     */
    fun reserve(couponId: Long, userId: Long): Boolean

    /**
     * 선점한 쿠폰 사용을 복원한다. (Redis DEL)
     */
    fun restore(couponId: Long, userId: Long)
}
