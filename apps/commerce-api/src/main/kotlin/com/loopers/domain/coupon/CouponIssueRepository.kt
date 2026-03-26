package com.loopers.domain.coupon

interface CouponIssueRepository {

    companion object {
        const val ISSUE_SUCCESS = 1L
        const val ISSUE_EXHAUSTED = 0L
        const val ISSUE_DUPLICATE = -1L
    }

    /**
     * 쿠폰 발급 요청을 기록한다.
     * @return ISSUE_SUCCESS(1): 성공, ISSUE_EXHAUSTED(0): 수량 소진, ISSUE_DUPLICATE(-1): 중복 요청
     */
    fun tryIssue(couponId: Long, userId: Long, maxQuantity: Int): Long

    /**
     * 발급 실패 시 유저를 제거하여 수량을 복원한다.
     */
    fun restore(couponId: Long, userId: Long)

    /**
     * 쿠폰 최대 발급 수량을 초기화한다.
     */
    fun initCouponStock(couponId: Long, maxQuantity: Int)
}
