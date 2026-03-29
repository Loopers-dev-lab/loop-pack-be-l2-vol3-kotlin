package com.loopers.domain.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 도메인 이벤트 발행 전담 컴포넌트.
 *
 * Facade에서 ApplicationEventPublisher를 직접 사용하지 않고
 * 이 클래스를 통해 발행하여:
 * - 발행 로직과 비즈니스 로직 분리
 * - 테스트 시 이 클래스만 mock하면 이벤트 발행 검증 가능
 * - 발행 전 공통 로직(로깅, 필터링) 추가 용이
 */
@Component
class DomainEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun publish(event: DomainEvent) {
        eventPublisher.publishEvent(event)
    }

    fun publishAll(events: List<DomainEvent>) {
        events.forEach { eventPublisher.publishEvent(it) }
    }
}
