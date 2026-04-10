package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.RankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository

/**
 * 랭킹 ZSET 쓰기 구현체.
 *
 * Master RedisTemplate을 사용하여 일간 + 시간 ZSET에 score를 동시에 갱신한다.
 * pipeline으로 batch ZADD(daily) + ZADD(hourly) + EXPIREAT 2건을 단일 왕복으로 처리한다.
 *
 * 주의: executePipelined 콜백 안에서 redisTemplate.opsForZSet() 같은 고수준 API를
 * 호출하면 새 커넥션을 획득해서 실행하므로 파이프라인이 적용되지 않는다.
 * 반드시 callback의 connection 파라미터를 직접 사용해야 한다.
 *
 * 만료는 절대 시각(EXPIREAT) 기반으로 고정된다.
 * - 일간 키: dateKey + 2일 자정
 * - 시간 키: hourKey + 2시간 정각
 */
@Repository
class RankingRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : RankingRepository {

    override fun updateScores(dateKey: String, hourKey: String, scores: Map<Long, Double>) {
        if (scores.isEmpty()) return

        val dailyKey = RankingRedisKeys.dailyKey(dateKey)
        val hourlyKey = RankingRedisKeys.hourlyKey(hourKey)
        val dailyKeyBytes = dailyKey.toByteArray(Charsets.UTF_8)
        val hourlyKeyBytes = hourlyKey.toByteArray(Charsets.UTF_8)
        val dailyExpireAt = RankingRedisKeys.dailyExpireAtEpochSecond(dateKey)
        val hourlyExpireAt = RankingRedisKeys.hourlyExpireAtEpochSecond(hourKey)

        redisTemplate.executePipelined { connection ->
            val zSetCommands = connection.zSetCommands()
            val keyCommands = connection.keyCommands()

            scores.forEach { (productId, score) ->
                val memberBytes = productId.toString().toByteArray(Charsets.UTF_8)
                zSetCommands.zAdd(dailyKeyBytes, score, memberBytes)
                zSetCommands.zAdd(hourlyKeyBytes, score, memberBytes)
            }
            keyCommands.expireAt(dailyKeyBytes, dailyExpireAt)
            keyCommands.expireAt(hourlyKeyBytes, hourlyExpireAt)

            null
        }
    }

    /**
     * ZUNIONSTORE fromKey WEIGHTS weight 로 toKey를 덮어쓴다.
     *
     * 일간 랭킹에만 적용된다 (시간 랭킹은 TTL 2시간으로 자연 소멸 유도).
     * Redis ZUNIONSTORE는 원자적으로 실행되며, toKey가 이미 존재하면 덮어쓴다.
     */
    override fun carryOverScores(fromDateKey: String, toDateKey: String, weight: Double) {
        val fromKey = RankingRedisKeys.dailyKey(fromDateKey)
        val toKey = RankingRedisKeys.dailyKey(toDateKey)

        redisTemplate.opsForZSet().unionAndStore(
            fromKey,
            emptyList<String>(),
            toKey,
            Aggregate.SUM,
            Weights.of(weight),
        )

        val expireAt = RankingRedisKeys.dailyExpireAtEpochSecond(toDateKey)
        val toKeyBytes = toKey.toByteArray(Charsets.UTF_8)
        redisTemplate.execute { connection ->
            connection.keyCommands().expireAt(toKeyBytes, expireAt)
        }
    }
}
