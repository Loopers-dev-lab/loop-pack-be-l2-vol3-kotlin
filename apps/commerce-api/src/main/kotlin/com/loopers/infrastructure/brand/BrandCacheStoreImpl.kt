package com.loopers.infrastructure.brand

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.brand.BrandInfo
import com.loopers.config.redis.RedisConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class BrandCacheStoreImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : BrandCacheStore {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BRAND_KEY_PREFIX = "brand:detail:"
        private val BRAND_TTL = Duration.ofMinutes(10)
    }

    override fun getBrand(brandId: Long): BrandInfo? {
        return try {
            val json = redisTemplate.opsForValue().get("$BRAND_KEY_PREFIX$brandId") ?: return null
            objectMapper.readValue(json, BrandInfo::class.java)
        } catch (e: Exception) {
            log.warn("[BrandCache] 브랜드 캐시 조회 실패 (brandId={})", brandId, e)
            null
        }
    }

    override fun putBrand(brandId: Long, info: BrandInfo) {
        try {
            masterRedisTemplate.opsForValue().set(
                "$BRAND_KEY_PREFIX$brandId",
                objectMapper.writeValueAsString(info),
                BRAND_TTL,
            )
        } catch (e: Exception) {
            log.warn("[BrandCache] 브랜드 캐시 저장 실패 (brandId={})", brandId, e)
        }
    }

    override fun evictBrand(brandId: Long) {
        try {
            masterRedisTemplate.delete("$BRAND_KEY_PREFIX$brandId")
        } catch (e: Exception) {
            log.warn("[BrandCache] 브랜드 캐시 삭제 실패 (brandId={})", brandId, e)
        }
    }
}
