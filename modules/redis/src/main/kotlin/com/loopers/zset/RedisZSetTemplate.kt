package com.loopers.zset

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisZSetTemplate(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val redisTemplate: RedisTemplate<String, String>,
) {

    fun incrementScore(key: String, member: String, delta: Double) {
        masterRedisTemplate.opsForZSet().incrementScore(key, member, delta)
    }

    fun reverseRangeWithScores(key: String, start: Long, end: Long): List<ZSetEntry> =
        toEntries(redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end))

    fun reverseRangeWithScoresFromMaster(key: String, start: Long, end: Long): List<ZSetEntry> =
        toEntries(masterRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end))

    private fun toEntries(tuples: Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>>?): List<ZSetEntry> {
        if (tuples == null) return emptyList()
        return tuples.mapNotNull { tuple ->
            val member = tuple.value ?: return@mapNotNull null
            val score = tuple.score ?: return@mapNotNull null
            ZSetEntry(member, score)
        }
    }

    fun reverseRank(key: String, member: String): Long? {
        return redisTemplate.opsForZSet().reverseRank(key, member)
    }

    fun score(key: String, member: String): Double? {
        return redisTemplate.opsForZSet().score(key, member)
    }

    fun size(key: String): Long {
        return redisTemplate.opsForZSet().size(key) ?: 0L
    }

    fun unionStoreWithWeight(destKey: String, sourceKey: String, weight: Double): Long {
        return masterRedisTemplate.execute { connection ->
            connection.zSetCommands().zUnionStore(
                masterRedisTemplate.stringSerializer.serialize(destKey)!!,
                Aggregate.SUM,
                Weights.of(weight),
                masterRedisTemplate.stringSerializer.serialize(sourceKey)!!,
            )
        } ?: 0L
    }

    fun rename(oldKey: String, newKey: String) {
        masterRedisTemplate.rename(oldKey, newKey)
    }

    fun delete(key: String) {
        masterRedisTemplate.delete(key)
    }

    fun setTtlIfAbsent(key: String, ttl: Duration) {
        val currentTtl = redisTemplate.getExpire(key) ?: -1L
        if (currentTtl == -1L) {
            masterRedisTemplate.expire(key, ttl)
        }
    }

    fun getExpireSeconds(key: String): Long = redisTemplate.getExpire(key) ?: -2L

    fun hasKey(key: String): Boolean = redisTemplate.hasKey(key) ?: false
}
