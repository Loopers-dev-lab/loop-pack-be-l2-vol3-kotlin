package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueEntryState
import com.loopers.application.queue.QueueExperimentProperties
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class QueueRedisSupport(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val queueExperimentProperties: QueueExperimentProperties,
    private val queueTokenGenerator: QueueTokenGenerator,
) {
    fun enter(strategyType: QueueStrategyType, memberId: Long): QueueInfo.Status {
        val admittedToken = getTokenInfo(strategyType, memberId)
        if (admittedToken != null) {
            return admittedStatus(strategyType, admittedToken.first, admittedToken.second)
        }

        val sequence = redisTemplate.opsForValue().increment(sequenceKey(strategyType)) ?: 0L
        redisTemplate.opsForZSet().addIfAbsent(waitingKey(strategyType), memberId.toString(), sequence.toDouble())
        return getStatus(strategyType, memberId)
    }

    fun getStatus(strategyType: QueueStrategyType, memberId: Long): QueueInfo.Status {
        val admittedToken = getTokenInfo(strategyType, memberId)
        if (admittedToken != null) {
            return admittedStatus(strategyType, admittedToken.first, admittedToken.second)
        }

        val rank = redisTemplate.opsForZSet().rank(waitingKey(strategyType), memberId.toString())
        val waitingCount = redisTemplate.opsForZSet().size(waitingKey(strategyType)) ?: 0L
        val position = rank?.plus(1)

        return QueueInfo.Status(
            strategy = strategyType,
            state = if (position != null) QueueEntryState.WAITING else QueueEntryState.NONE,
            position = position,
            totalWaitingCount = waitingCount,
            expectedWaitSeconds = expectedWaitSeconds(position),
            token = null,
            tokenExpiresAt = null,
        )
    }

    fun admit(strategyType: QueueStrategyType, batchSize: Int, tokenConsumer: (Long, String, ZonedDateTime) -> Unit): Int {
        val members = redisTemplate.opsForZSet().range(waitingKey(strategyType), 0, (batchSize - 1).toLong())
            ?.map(String::toLong)
            .orEmpty()
        if (members.isEmpty()) {
            return 0
        }

        members.forEach { memberId ->
            if ((redisTemplate.opsForZSet().remove(waitingKey(strategyType), memberId.toString()) ?: 0L) > 0L) {
                val token = queueTokenGenerator.generate()
                val expiresAt = ZonedDateTime.now().plus(queueExperimentProperties.tokenTtl)
                tokenConsumer(memberId, token, expiresAt)
            }
        }
        return members.size
    }

    fun issueToken(
        strategyType: QueueStrategyType,
        memberId: Long,
        token: String,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plus(queueExperimentProperties.tokenTtl),
    ) {
        redisTemplate.opsForValue().set(
            tokenKey(strategyType, memberId),
            token,
            queueExperimentProperties.tokenTtl,
        )
        redisTemplate.opsForValue().set(
            expiresAtKey(strategyType, memberId),
            expiresAt.toString(),
            queueExperimentProperties.tokenTtl,
        )
    }

    fun validateToken(strategyType: QueueStrategyType, memberId: Long, token: String): Boolean {
        return redisTemplate.opsForValue().get(tokenKey(strategyType, memberId)) == token
    }

    fun complete(strategyType: QueueStrategyType, memberId: Long) {
        redisTemplate.delete(tokenKey(strategyType, memberId))
        redisTemplate.delete(expiresAtKey(strategyType, memberId))
    }

    private fun admittedStatus(
        strategyType: QueueStrategyType,
        token: String,
        expiresAt: ZonedDateTime,
    ): QueueInfo.Status {
        return QueueInfo.Status(
            strategy = strategyType,
            state = QueueEntryState.ADMITTED,
            position = 0,
            totalWaitingCount = redisTemplate.opsForZSet().size(waitingKey(strategyType)) ?: 0L,
            expectedWaitSeconds = 0L,
            token = token,
            tokenExpiresAt = expiresAt,
        )
    }

    private fun getTokenInfo(strategyType: QueueStrategyType, memberId: Long): Pair<String, ZonedDateTime>? {
        val token = redisTemplate.opsForValue().get(tokenKey(strategyType, memberId)) ?: return null
        val expiresAt = redisTemplate.opsForValue().get(expiresAtKey(strategyType, memberId))
            ?.let(ZonedDateTime::parse)
            ?: ZonedDateTime.now().plus(queueExperimentProperties.tokenTtl)
        return token to expiresAt
    }

    private fun expectedWaitSeconds(position: Long?): Long {
        position ?: return 0L
        val batchesAhead = (position - 1) / queueExperimentProperties.resolvedBatchSize() + 1
        return batchesAhead * queueExperimentProperties.scheduler.fixedDelay.seconds
    }

    private fun waitingKey(strategyType: QueueStrategyType): String {
        return "queue:${strategyType.name.lowercase()}:waiting"
    }

    private fun tokenKey(strategyType: QueueStrategyType, memberId: Long): String {
        return "queue:${strategyType.name.lowercase()}:token:$memberId"
    }

    private fun sequenceKey(strategyType: QueueStrategyType): String {
        return "queue:${strategyType.name.lowercase()}:sequence"
    }

    private fun expiresAtKey(strategyType: QueueStrategyType, memberId: Long): String {
        return "queue:${strategyType.name.lowercase()}:expires-at:$memberId"
    }
}
