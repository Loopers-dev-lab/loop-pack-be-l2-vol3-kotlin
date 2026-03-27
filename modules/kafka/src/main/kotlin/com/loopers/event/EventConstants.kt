package com.loopers.event

/**
 * Kafka 이벤트 관련 상수.
 *
 * commerce-api(발행)와 commerce-streamer(소비) 양쪽에서 참조하므로
 * 공유 kafka 모듈에 정의한다.
 */
object Topics {
    const val CATALOG = "catalog-events"
    const val ORDER = "order-events"
    const val COUPON_ISSUE = "coupon-issue-requests"
}

object AggregateTypes {
    const val CATALOG = "CATALOG"
    const val ORDER = "ORDER"
    const val COUPON = "COUPON"
}

object EventTypes {
    const val LIKED = "LIKED"
    const val UNLIKED = "UNLIKED"
    const val VIEWED = "VIEWED"
    const val ORDER_COMPLETED = "ORDER_COMPLETED"
    const val COUPON_ISSUE_REQUESTED = "COUPON_ISSUE_REQUESTED"
}
