package com.loopers.infrastructure.ranking

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingWeights
import com.loopers.domain.ranking.RankingWeightsRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

/**
 * 랭킹 가중치 읽기/쓰기 구현체 (commerce-api).
 *
 * 쓰기는 Master RedisTemplate 사용.
 * JSON 직렬화 포맷으로 단일 Key에 저장한다.
 */
@Repository
class RankingWeightsRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : RankingWeightsRepository {

    companion object {
        private val log = LoggerFactory.getLogger(RankingWeightsRepositoryImpl::class.java)
    }

    override fun findCurrent(): RankingWeights? {
        val json = redisTemplate.opsForValue().get(RankingWeightsRedisKeys.KEY) ?: return null

        return try {
            objectMapper.readValue(json, RankingWeights::class.java)
        } catch (e: Exception) {
            log.warn("[랭킹] 가중치 JSON 파싱 실패 [json={}]", json, e)
            null
        }
    }

    override fun save(weights: RankingWeights) {
        val json = objectMapper.writeValueAsString(weights)
        redisTemplate.opsForValue().set(RankingWeightsRedisKeys.KEY, json)
        log.info("[랭킹] 가중치 저장 완료 [weights={}]", weights)
    }
}
