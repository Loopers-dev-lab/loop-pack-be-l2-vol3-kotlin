package com.loopers.domain.ratelimit

/**
 * 중복 요청 방지 어노테이션.
 *
 * Redis SETNX 기반으로 동일 키의 중복 호출을 차단한다.
 * 중복 요청 시 예외 없이 조용히 무시 (메서드 실행 건너뜀).
 *
 * - key 템플릿에서 {paramName}은 메서드 파라미터 값으로 치환됨
 * - @Transactional보다 먼저 실행되어, 중복 시 트랜잭션 자체가 시작되지 않음
 * - Redis 장애 시 graceful degradation (통과 처리)
 *
 * 사용 예:
 * ```
 * @RateLimit(key = "like:{userId}:{productId}", ttl = 1)
 * @Transactional
 * fun like(userId: Long, productId: Long) { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    /** 키 템플릿. {paramName}은 메서드 파라미터로 치환됨 */
    val key: String,
    /** TTL (초). 이 시간 동안 동일 키의 재요청을 차단 */
    val ttl: Long = 3,
)
