package com.loopers.application.outbox

/**
 * Outbox 이벤트 발행 인터페이스.
 *
 * application 레이어에서 infrastructure에 의존하지 않도록 DIP를 적용한다.
 */
interface OutboxPublisher {
    fun publish(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        version: Long,
        payload: Any,
    )
}
