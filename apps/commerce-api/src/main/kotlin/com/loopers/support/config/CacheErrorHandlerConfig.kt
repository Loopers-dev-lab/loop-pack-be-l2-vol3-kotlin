package com.loopers.support.config

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Configuration

@Configuration
class CacheErrorHandlerConfig : CachingConfigurerSupport() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun errorHandler(): CacheErrorHandler = GracefulCacheErrorHandler(logger)
}

class GracefulCacheErrorHandler(
    private val logger: org.slf4j.Logger,
) : CacheErrorHandler {
    /**
     * 캐시 조회 에러 발생시:
     * - WARN 로그 기록
     * - 예외는 무시하고 메서드 실행 계속 진행 (DB에서 직접 조회)
     */
    override fun handleCacheGetError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any) {
        logger.warn("Cache GET failed for key: $key in cache: ${cache.name}. Falling back to direct DB lookup.", exception)
    }

    /**
     * 캐시 저장 에러 발생시:
     * - WARN 로그 기록
     * - 예외는 무시 (데이터는 정상 반환되었음)
     */
    override fun handleCachePutError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any, value: Any?) {
        logger.warn("Cache PUT failed for key: $key in cache: ${cache.name}. Data will not be cached.", exception)
    }

    /**
     * 캐시 삭제 에러 발생시:
     * - WARN 로그 기록
     * - 예외는 무시 (데이터는 이미 업데이트됨)
     */
    override fun handleCacheEvictError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any) {
        logger.warn("Cache EVICT failed for key: $key in cache: ${cache.name}. Cache may contain stale data.", exception)
    }

    /**
     * 캐시 전체 초기화 에러 발생시:
     * - WARN 로그 기록
     * - 예외는 무시
     */
    override fun handleCacheClearError(exception: RuntimeException, cache: org.springframework.cache.Cache) {
        logger.warn("Cache CLEAR failed for cache: ${cache.name}.", exception)
    }
}
