package com.loopers.domain.cache

/**
 * 캐시 레벨 구분.
 *
 * - LOCAL: JVM 내부(Caffeine)만 사용 — 변경 거의 없는 설정값
 * - GLOBAL: Redis만 사용 — 인스턴스 간 공유 필요
 * - COMPOSITE: Caffeine → Redis 계층적 조회 (핵심)
 */
enum class CacheType {
    LOCAL,
    GLOBAL,
    COMPOSITE,
}
