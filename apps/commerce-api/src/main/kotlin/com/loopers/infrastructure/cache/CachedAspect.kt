package com.loopers.infrastructure.cache

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.loopers.domain.cache.Cached
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

/**
 * @Cached 어노테이션 처리 AOP Aspect.
 *
 * CompositeCacheManager가 cacheName에 따라 적절한 Cache를 반환하므로,
 * Aspect는 L1/L2를 몰라도 된다. Cache-Aside 패턴만 수행.
 *
 * 흐름: cache.get(key) → 히트면 반환 → 미스면 DB 조회 → cache.put(key, value)
 *
 * 역직렬화 시 메서드 리턴 타입을 [TypeAwareRedisSerializer]에 ThreadLocal로 전달하여,
 * JSON에 "@class" 메타데이터 없이도 정확한 타입으로 복원한다.
 *
 * Redis 장애 시 Graceful Degradation — 캐시 실패해도 DB 조회 진행.
 */
@Aspect
@Component
class CachedAspect(
    private val cacheManager: CacheManager,
) {

    companion object {
        private val log = LoggerFactory.getLogger(CachedAspect::class.java)
        private val typeFactory = TypeFactory.defaultInstance()
    }

    @Around("@annotation(cached)")
    fun around(joinPoint: ProceedingJoinPoint, cached: Cached): Any? {
        val cache = getCache(cached.cacheName) ?: return joinPoint.proceed()
        val resolvedKey = CacheKeyResolver.resolve(cached.key, joinPoint)
        val returnType = resolveReturnType(joinPoint)

        // 1. 캐시 조회 — 리턴 타입 힌트 설정
        val cachedValue = getCacheValue(cache, resolvedKey, returnType)
        if (cachedValue != null) return cachedValue

        // 2. 캐시 미스 → DB 조회
        val result = joinPoint.proceed()

        // 3. 캐시 저장
        if (result != null) putCache(cache, resolvedKey, result)
        return result
    }

    /**
     * 메서드 리턴 타입을 JavaType으로 변환.
     *
     * 제네릭 타입(ProductListCacheResult 내부의 List<ProductResult> 등)도
     * genericReturnType에서 정확히 추출된다.
     */
    private fun resolveReturnType(joinPoint: ProceedingJoinPoint): JavaType {
        val method = (joinPoint.signature as MethodSignature).method
        return typeFactory.constructType(method.genericReturnType)
    }

    /** CacheManager에서 Cache 조회. 실패 시 null (Graceful Degradation) */
    private fun getCache(cacheName: String): Cache? {
        return try {
            cacheManager.getCache(cacheName)
        } catch (e: Exception) {
            log.warn("캐시 매니저 조회 실패 [cacheName={}]", cacheName, e)
            null
        }
    }

    /**
     * 캐시에서 값 조회. 리턴 타입 힌트를 ThreadLocal에 설정하여
     * TypeAwareRedisSerializer가 정확한 타입으로 역직렬화하도록 한다.
     *
     * 실패 시 null (Graceful Degradation)
     */
    private fun getCacheValue(cache: Cache, key: String, returnType: JavaType): Any? {
        return try {
            TypeAwareRedisSerializer.setExpectedType(returnType)
            cache.get(key)?.get()
        } catch (e: Exception) {
            log.warn("캐시 조회 실패, DB 조회 진행 [key={}]", key, e)
            null
        } finally {
            TypeAwareRedisSerializer.clearExpectedType()
        }
    }

    /** 캐시에 값 저장. 실패해도 예외 전파 안 함 (Graceful Degradation) */
    private fun putCache(cache: Cache, key: String, value: Any) {
        try {
            cache.put(key, value)
        } catch (e: Exception) {
            log.warn("캐시 저장 실패 [key={}]", key, e)
        }
    }
}
