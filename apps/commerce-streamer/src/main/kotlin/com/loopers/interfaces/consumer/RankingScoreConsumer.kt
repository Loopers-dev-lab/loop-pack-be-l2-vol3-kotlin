package com.loopers.interfaces.consumer

import com.loopers.config.RankingProperties
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopicConfig
import com.loopers.config.redis.RedisConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.log10

@Component
class RankingScoreConsumer(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val rankingProperties: RankingProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dailyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val hourlyFormatter = DateTimeFormatter.ofPattern("HH")

    @KafkaListener(
        topics = [KafkaTopicConfig.PRODUCT_ACTION_TOPIC],
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = CONSUMER_GROUP,
    )
    fun onProductActions(
        records: List<ConsumerRecord<String, ByteArray>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val now = LocalDateTime.now(ZONE_ID)
            val dailyKey = "${rankingProperties.keyPrefix}:${now.format(dailyFormatter)}"
            val hourlyKey = "${rankingProperties.keyPrefix}:${now.format(dailyFormatter)}:${now.format(hourlyFormatter)}"

            val scores = mutableMapOf<Long, Double>()
            for (record in records) {
                val parsed = parseRecord(record) ?: continue
                val score = calculateScore(parsed)
                scores.merge(parsed.targetId!!, score, Double::plus)
            }

            flushScores(dailyKey, scores, rankingProperties.ttl.daily)
            flushScores(hourlyKey, scores, rankingProperties.ttl.hourly)

            log.debug(
                "랭킹 점수 갱신 완료: records={}, products={}, dailyKey={}, hourlyKey={}",
                records.size,
                scores.size,
                dailyKey,
                hourlyKey,
            )
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("랭킹 점수 처리 실패 (ack 미수행, 재전달 예정): batchSize={}, error={}", records.size, e.message, e)
        }
    }

    private fun parseRecord(record: ConsumerRecord<String, ByteArray>): ProductActionPayload? {
        return try {
            val payload = objectMapper.readValue(record.value(), ProductActionPayload::class.java)
            if (payload.actionType == null || payload.targetId == null) return null
            payload
        } catch (e: Exception) {
            log.warn("랭킹 이벤트 파싱 실패: record={}", String(record.value()), e)
            null
        }
    }

    private fun calculateScore(payload: ProductActionPayload): Double {
        return when (payload.actionType) {
            "VIEW" -> rankingProperties.weights.view
            "LIKE" -> rankingProperties.weights.like
            "ORDER" -> {
                val price = payload.price ?: 1L
                val quantity = payload.quantity ?: 1
                val orderValue = (price * quantity).toDouble()
                rankingProperties.weights.order * log10(orderValue + 1)
            }
            else -> 0.0
        }
    }

    private fun flushScores(key: String, scores: Map<Long, Double>, ttl: Duration) {
        if (scores.isEmpty()) return

        redisTemplate.executePipelined { connection ->
            val zSetCommands = connection.zSetCommands()
            val keyBytes = key.toByteArray()
            scores.forEach { (productId, score) ->
                zSetCommands.zIncrBy(keyBytes, score, productId.toString().toByteArray())
            }
            null
        }
        redisTemplate.expire(key, ttl)
    }

    data class ProductActionPayload(
        val memberId: Long? = null,
        val actionType: String? = null,
        val targetType: String? = null,
        val targetId: Long? = null,
        val price: Long? = null,
        val quantity: Int? = null,
    )

    companion object {
        const val CONSUMER_GROUP = "commerce-streamer-ranking"
        private val ZONE_ID = ZoneId.of("Asia/Seoul")
    }
}
