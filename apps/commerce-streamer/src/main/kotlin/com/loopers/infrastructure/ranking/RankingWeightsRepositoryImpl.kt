package com.loopers.infrastructure.ranking

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.ranking.RankingWeights
import com.loopers.domain.ranking.RankingWeightsRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

/**
 * 랭킹 가중치 읽기 구현체 (commerce-streamer).
 *
 * 기본 RedisTemplate(REPLICA_PREFERRED)을 사용.
 * JSON 직렬화로 저장되어 있는 가중치를 역직렬화한다.
 * 값이 없거나 파싱 실패 시 null 반환 (RankingProcessor에서 DEFAULT fallback).
 */
@Repository
class RankingWeightsRepositoryImpl(
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
}
