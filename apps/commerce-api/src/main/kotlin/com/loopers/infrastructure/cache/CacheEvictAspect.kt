package com.loopers.infrastructure.cache

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * @CacheEvict 어노테이션 처리 AOP Aspect.
 *
 * CompositeCacheManager가 CacheType에 따라 적절한 Cache를 반환하므로,
 * Aspect는 cache.evict(key) 한 줄이면 된다. (CompositeCache가 L1+L2 모두 삭제)
 *
 * - 즉시 삭제 (afterCommit 방식 대신)
 *   → afterCommit은 같은 트랜잭션 내 write→read 시 stale cache 위험이 있음
 *   → 즉시 삭제 시 롤백되면 캐시가 비어있지만, 다음 조회에서 DB 값으로 재충전되므로 정합성 문제 없음
 * - Redis 장애 시 Graceful Degradation
 */
@Aspect
@Component
class CacheEvictAspect(
    private val cacheManager: CacheManager,
) {

    companion object {
        private val log = LoggerFactory.getLogger(CacheEvictAspect::class.java)
    }

    @AfterReturning("@annotation(cacheEvict)")
    fun afterReturning(joinPoint: JoinPoint, cacheEvict: com.loopers.domain.cache.CacheEvict) {
        val resolvedKey = CacheKeyResolver.resolve(cacheEvict.key, joinPoint)
        val cacheName = cacheEvict.cacheName

        val cache = getCache(cacheName) ?: return
        evict(cache, cacheName, resolvedKey)
    }

    private fun getCache(cacheName: String): Cache? {
        return try {
            cacheManager.getCache(cacheName)
        } catch (e: Exception) {
            log.warn("캐시 매니저 조회 실패 [cacheName={}]", cacheName, e)
            null
        }
    }

    private fun evict(cache: Cache, cacheName: String, key: String) {
        try {
            cache.evict(key)
            log.debug("캐시 삭제 완료 [cacheName={}, key={}]", cacheName, key)
        } catch (e: Exception) {
            log.warn("캐시 삭제 실패 [cacheName={}, key={}]", cacheName, key, e)
        }
    }

}
