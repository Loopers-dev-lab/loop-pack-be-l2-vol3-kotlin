package com.loopers.infrastructure.cache

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/**
 * L1 (Caffeine) + L2 (Redis) 캐시 매니저 설정.
 *
 * 캐시 메타데이터는 [CacheName] enum에서 중앙 관리.
 * 캐시 추가 시 CacheName에 1줄 추가하면 L1/L2 TTL + CacheType이 모두 적용된다.
 *
 * - L1: CaffeineCacheManager — JVM 내부, ns급 조회, 짧은 TTL
 * - L2: RedisCacheManager — 분산 캐시, 긴 TTL, TypeAwareRedisSerializer 직렬화
 * - Redis 키 형식: "cache:{cacheName}::{actualKey}"
 */
@Configuration
class CacheConfig {

    companion object {
        const val LOCAL_CACHE_MANAGER = "localCacheManager"
        const val GLOBAL_CACHE_MANAGER = "globalCacheManager"
        const val COMPOSITE_CACHE_MANAGER = "compositeCacheManager"

        private const val NAMESPACE = "cache"
        private val DEFAULT_L2_TTL = Duration.ofMinutes(10)
    }

    /**
     * L1 — CaffeineCacheManager.
     *
     * [CacheName.l1Entries]에서 L1 사용 캐시만 필터하여 등록.
     */
    @Bean(LOCAL_CACHE_MANAGER)
    fun localCacheManager(): CaffeineCacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCacheSpecification("maximumSize=1000")

        val l1Caches = CacheName.l1Entries
        cacheManager.cacheNames = l1Caches.map { it.key }

        l1Caches.forEach { cache ->
            cacheManager.registerCustomCache(
                cache.key,
                Caffeine.newBuilder()
                    .maximumSize(500)
                    .expireAfterWrite(cache.l1Ttl!!)
                    .recordStats()
                    .build(),
            )
        }

        return cacheManager
    }

    /**
     * L2 — RedisCacheManager.
     *
     * [CacheName.entries]에서 L2 TTL을 읽어 cacheName별 설정 생성.
     */
    @Bean(GLOBAL_CACHE_MANAGER)
    fun globalCacheManager(
        redisConnectionFactory: RedisConnectionFactory,
        jacksonModules: List<Module>,
    ): RedisCacheManager {
        // Spring의 기본 ObjectMapper와 별도로 Redis 전용 ObjectMapper를 구성한다.
        // activateDefaultTyping 없이 순수 JSON을 저장한다.
        // 역직렬화 시 CachedAspect가 메서드 리턴 타입을 ThreadLocal로 전달하므로,
        // "@class" 메타데이터 없이도 정확한 타입으로 복원 가능.
        //
        // 모듈 등록은 2단계:
        // 1) findAndRegisterModules() — ServiceLoader로 KotlinModule, JavaTimeModule 등 발견
        // 2) registerModules(jacksonModules) — Spring Bean으로 등록된 모듈(PageModule 등) 추가
        //    (JavaTimeModule은 Bean이 아니라 ServiceLoader 경로이므로, 1단계 없으면 누락됨)
        val redisObjectMapper = ObjectMapper().apply {
            findAndRegisterModules()
            registerModules(jacksonModules)
            // 날짜를 timestamp(숫자) 대신 ISO-8601 문자열로 저장 (ex: "2026-03-11T15:30:27Z")
            // → Redis에서 직접 값을 확인할 때 가독성 확보
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        val jsonSerializer = TypeAwareRedisSerializer(redisObjectMapper)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .computePrefixWith { cacheName -> "$NAMESPACE:$cacheName::" }
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer),
            )
            .entryTtl(DEFAULT_L2_TTL)

        val perCacheConfig = CacheName.entries.associate { cache ->
            cache.key to defaultConfig.entryTtl(cache.l2Ttl)
        }

        // Cache Stampede 방지: 엔트리별 TTL Jitter 적용
        val jitterMap = CacheName.entries.associate { it.key to it.l2JitterSeconds }
        val cacheWriter = JitteredRedisCacheWriter(redisConnectionFactory, jitterMap)

        return RedisCacheManager.builder(cacheWriter)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(perCacheConfig)
            .build()
    }

    /**
     * 통합 CacheManager — CacheType에 따라 적절한 Cache 반환.
     *
     * [CacheName.cacheTypeMap]에서 cacheName → CacheType 매핑을 참조.
     * Aspect는 이 CacheManager만 주입받으면 된다.
     */
    @Primary
    @Bean(COMPOSITE_CACHE_MANAGER)
    fun compositeCacheManager(
        @Qualifier(LOCAL_CACHE_MANAGER) localCacheManager: CacheManager,
        @Qualifier(GLOBAL_CACHE_MANAGER) globalCacheManager: CacheManager,
    ): CacheManager {
        return CompositeCacheManager(
            localCacheManager = localCacheManager,
            globalCacheManager = globalCacheManager,
            cacheTypeMap = CacheName.cacheTypeMap,
        )
    }
}
