package com.loopers.infrastructure.cache

import com.loopers.domain.cache.CacheType
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * CacheType에 따라 적절한 Cache 인스턴스를 반환하는 통합 CacheManager.
 *
 * - COMPOSITE → [CompositeCache] (L1 + L2 계층적 조회)
 * - LOCAL → CaffeineCacheManager에서 조회
 * - GLOBAL → RedisCacheManager에서 조회
 *
 * Aspect는 이 CacheManager만 주입받으면 되므로, L1/L2를 몰라도 된다.
 */
class CompositeCacheManager(
    private val localCacheManager: CacheManager,
    private val globalCacheManager: CacheManager,
    private val cacheTypeMap: Map<String, CacheType>,
) : CacheManager {

    override fun getCache(name: String): Cache? {
        return when (cacheTypeMap[name] ?: CacheType.GLOBAL) {
            CacheType.COMPOSITE -> CompositeCache(
                cacheName = name,
                l1 = localCacheManager.getCache(name),
                l2 = globalCacheManager.getCache(name),
            )
            CacheType.LOCAL -> localCacheManager.getCache(name)
            CacheType.GLOBAL -> globalCacheManager.getCache(name)
        }
    }

    override fun getCacheNames(): Collection<String> {
        val names = mutableSetOf<String>()
        names.addAll(localCacheManager.cacheNames)
        names.addAll(globalCacheManager.cacheNames)
        return names
    }
}
