package com.loopers.infrastructure.cache

import com.loopers.domain.cache.CacheType
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import java.util.concurrent.ConcurrentHashMap

/**
 * CacheType에 따라 적절한 Cache 인스턴스를 반환하는 통합 CacheManager.
 *
 * - COMPOSITE → [CompositeCache] (L1 + L2 계층적 조회)
 * - LOCAL → CaffeineCacheManager에서 조회
 * - GLOBAL → RedisCacheManager에서 조회
 *
 * Aspect는 이 CacheManager만 주입받으면 되므로, L1/L2를 몰라도 된다.
 * Cache 인스턴스는 cacheName별로 한 번만 생성하여 재사용한다.
 */
class CompositeCacheManager(
    private val localCacheManager: CacheManager,
    private val globalCacheManager: CacheManager,
    private val cacheTypeMap: Map<String, CacheType>,
) : CacheManager {

    private val cacheInstances = ConcurrentHashMap<String, Cache>()

    override fun getCache(name: String): Cache? {
        return cacheInstances.computeIfAbsent(name) { cacheName ->
            when (cacheTypeMap[cacheName] ?: CacheType.GLOBAL) {
                CacheType.COMPOSITE -> CompositeCache(
                    cacheName = cacheName,
                    l1 = localCacheManager.getCache(cacheName),
                    l2 = globalCacheManager.getCache(cacheName),
                )
                CacheType.LOCAL -> localCacheManager.getCache(cacheName)!!
                CacheType.GLOBAL -> globalCacheManager.getCache(cacheName)!!
            }
        }
    }

    override fun getCacheNames(): Collection<String> {
        val names = mutableSetOf<String>()
        names.addAll(localCacheManager.cacheNames)
        names.addAll(globalCacheManager.cacheNames)
        return names
    }
}
