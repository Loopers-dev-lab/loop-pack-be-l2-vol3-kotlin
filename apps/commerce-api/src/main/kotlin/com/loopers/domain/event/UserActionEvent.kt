package com.loopers.domain.event

/**
 * 유저 행동 로깅 이벤트.
 *
 * 상품 조회, 좋아요, 주문 등 유저 행동을 서버 레벨에서 기록하기 위한 이벤트.
 * Kafka 발행 대상이 아닌 내부 처리 전용 (ApplicationEvent만 사용).
 */
data class UserActionEvent(
    override val aggregateId: Long,
    val userId: Long,
    val actionType: ActionType,
    val targetId: Long,
    val metadata: Map<String, String> = emptyMap(),
) : DomainEvent() {
    override val eventType: EventType = EventType.USER_ACTION
}

enum class ActionType {
    VIEW,
    LIKE,
    UNLIKE,
    ORDER,
}
