package com.loopers.infrastructure.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.auth.AuthCacheStore
import com.loopers.application.auth.CachedAuth
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AuthCacheStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : AuthCacheStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val AUTH_KEY_PREFIX = "auth:"
        private val AUTH_TTL = Duration.ofMinutes(5)
    }

    override fun getAuth(loginId: String): CachedAuth? {
        return try {
            val json = redisTemplate.opsForValue().get("$AUTH_KEY_PREFIX$loginId") ?: return null
            objectMapper.readValue(json, CachedAuth::class.java)
        } catch (e: Exception) {
            log.warn("[AuthCache] 인증 캐시 조회 실패 (loginId={})", loginId, e)
            null
        }
    }

    override fun putAuth(loginId: String, cachedAuth: CachedAuth) {
        try {
            masterRedisTemplate.opsForValue().set(
                "$AUTH_KEY_PREFIX$loginId",
                objectMapper.writeValueAsString(cachedAuth),
                AUTH_TTL,
            )
        } catch (e: Exception) {
            log.warn("[AuthCache] 인증 캐시 저장 실패 (loginId={})", loginId, e)
        }
    }

    override fun evictAuth(loginId: String) {
        try {
            masterRedisTemplate.delete("$AUTH_KEY_PREFIX$loginId")
        } catch (e: Exception) {
            log.warn("[AuthCache] 인증 캐시 삭제 실패 (loginId={})", loginId, e)
        }
    }
}
