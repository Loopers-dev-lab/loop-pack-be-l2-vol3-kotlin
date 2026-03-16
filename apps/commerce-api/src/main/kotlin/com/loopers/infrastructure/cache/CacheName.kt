package com.loopers.infrastructure.cache

import com.loopers.domain.cache.CacheNames
import com.loopers.domain.cache.CacheType
import java.time.Duration

/**
 * 캐시 메타데이터 중앙 관리.
 *
 * 캐시 추가 시 이 enum에 1줄만 추가하면 L1 TTL, L2 TTL, Jitter, CacheType이 모두 설정된다.
 *
 * @property key Redis/Caffeine에서 사용하는 캐시 이름 문자열
 * @property l1Ttl L1(Caffeine) TTL. null이면 L1 미사용
 * @property l2Ttl L2(Redis) 기본 TTL
 * @property l2JitterSeconds L2 TTL에 추가되는 랜덤 범위(초). Cache Stampede 방지용
 * @property cacheType LOCAL / GLOBAL / COMPOSITE
 */
enum class CacheName(
    val key: String,
    val l1Ttl: Duration?,
    val l2Ttl: Duration,
    val l2JitterSeconds: Long,
    val cacheType: CacheType,
) {
    PRODUCT_INFO(CacheNames.PRODUCT_INFO, Duration.ofMinutes(3), Duration.ofMinutes(10), 60, CacheType.COMPOSITE),
    BRAND_INFO(CacheNames.BRAND_INFO, Duration.ofMinutes(5), Duration.ofMinutes(30), 120, CacheType.COMPOSITE),
    PRODUCT_LIST(CacheNames.PRODUCT_LIST, null, Duration.ofMinutes(5), 30, CacheType.GLOBAL),
    ;

    companion object {
        /** cacheName 문자열 → CacheType 매핑. CompositeCacheManager에서 사용 */
        val cacheTypeMap: Map<String, CacheType> =
            entries.associate { it.key to it.cacheType }

        /** L1 사용 캐시만 필터 (l1Ttl != null) */
        val l1Entries: List<CacheName> =
            entries.filter { it.l1Ttl != null }
    }
}
