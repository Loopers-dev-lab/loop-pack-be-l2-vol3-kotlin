package com.loopers.infrastructure.cache

import org.slf4j.LoggerFactory
import org.springframework.cache.Cache
import java.util.concurrent.Callable

/**
 * L1(Caffeine) + L2(Redis) 계층적 캐시 구현체.
 *
 * Spring [Cache] 인터페이스를 구현하여, Aspect는 L1/L2를 몰라도 된다.
 *
 * 조회 흐름:
 * ```
 * get(key) → L1 조회 → HIT → 반환
 *                    → MISS → L2 조회 → HIT → L1에 back-fill + 반환
 *                                     → MISS → null
 * ```
 *
 * 쓰기: L1 + L2 모두 저장
 * 삭제: L1 + L2 모두 삭제
 * Graceful Degradation: L2 실패 시 예외 전파 안 함
 */
class CompositeCache(
    private val cacheName: String,
    private val l1: Cache?,
    private val l2: Cache?,
) : Cache {

    companion object {
        private val log = LoggerFactory.getLogger(CompositeCache::class.java)
    }

    override fun getName(): String = cacheName

    override fun getNativeCache(): Any = this

    /**
     * L1 → L2 순서로 조회. L2 히트 시 L1에 back-fill.
     */
    override fun get(key: Any): Cache.ValueWrapper? {
        // 1. L1 조회
        val l1Value = safeGet(l1, key, "L1")
        if (l1Value != null) {
            log.debug("L1 HIT [cacheName={}, key={}]", cacheName, key)
            return l1Value
        }

        // 2. L2 조회
        val l2Value = safeGet(l2, key, "L2")
        if (l2Value != null) {
            log.debug("L2 HIT → L1 back-fill [cacheName={}, key={}]", cacheName, key)
            safePutIfAbsent(l1, key, l2Value.get()) // L1에 동기화
            return l2Value
        }

        return null
    }

    override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
        val wrapper = get(key) ?: return null
        @Suppress("UNCHECKED_CAST")
        return wrapper.get() as? T
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
        val wrapper = get(key)
        if (wrapper != null) {
            @Suppress("UNCHECKED_CAST")
            return wrapper.get() as? T
        }

        // 모든 캐시 미스 → valueLoader 실행 후 저장
        val value = valueLoader.call()
        if (value != null) put(key, value)
        return value
    }

    /** L1 + L2 모두 저장 */
    override fun put(key: Any, value: Any?) {
        safePut(l1, key, value, "L1")
        safePut(l2, key, value, "L2")
    }

    override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
        safePutIfAbsent(l1, key, value)
        safePutIfAbsent(l2, key, value)
        return null
    }

    /** L1 + L2 모두 삭제 */
    override fun evict(key: Any) {
        safeEvict(l1, key, "L1")
        safeEvict(l2, key, "L2")
    }

    /** L1 + L2 모두 초기화 */
    override fun clear() {
        safeClear(l1, "L1")
        safeClear(l2, "L2")
    }

    // --- Graceful Degradation helpers ---

    private fun safeGet(cache: Cache?, key: Any, level: String): Cache.ValueWrapper? {
        if (cache == null) return null
        return try {
            cache.get(key)
        } catch (e: Exception) {
            log.warn("캐시 조회 실패 [{}, cacheName={}, key={}]", level, cacheName, key, e)
            null
        }
    }

    private fun safePut(cache: Cache?, key: Any, value: Any?, level: String) {
        if (cache == null) return
        try {
            cache.put(key, value)
        } catch (e: Exception) {
            log.warn("캐시 저장 실패 [{}, cacheName={}, key={}]", level, cacheName, key, e)
        }
    }

    private fun safePutIfAbsent(cache: Cache?, key: Any, value: Any?) {
        if (cache == null) return
        try {
            cache.putIfAbsent(key, value)
        } catch (e: Exception) {
            log.warn("캐시 back-fill 실패 [cacheName={}, key={}]", cacheName, key, e)
        }
    }

    private fun safeEvict(cache: Cache?, key: Any, level: String) {
        if (cache == null) return
        try {
            cache.evict(key)
        } catch (e: Exception) {
            log.warn("캐시 삭제 실패 [{}, cacheName={}, key={}]", level, cacheName, key, e)
        }
    }

    private fun safeClear(cache: Cache?, level: String) {
        if (cache == null) return
        try {
            cache.clear()
        } catch (e: Exception) {
            log.warn("캐시 초기화 실패 [{}, cacheName={}]", level, cacheName, e)
        }
    }
}
