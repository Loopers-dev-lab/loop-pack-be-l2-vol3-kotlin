package com.loopers.application.metrics

import com.loopers.application.ranking.RankingProperties
import com.loopers.common.DateUtils
import com.loopers.hash.MetricsDailyKey
import com.loopers.hash.RedisHashTemplate
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCountFlushScheduler(
    private val productCountBuffer: ProductCountBuffer,
    private val redisHashTemplate: RedisHashTemplate,
    private val rankingProperties: RankingProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PreDestroy
    fun onShutdown() {
        log.info("서버 종료 감지 — 상품 카운트 버퍼 강제 flush 시작")
        flush()
        log.info("서버 종료 감지 — 상품 카운트 버퍼 강제 flush 완료")
    }

    @Scheduled(fixedRate = 5_000)
    fun flush() {
        val counts = productCountBuffer.drainAll()
        if (counts.isEmpty()) return

        try {
            val key = MetricsDailyKey.key(DateUtils.todayKst())
            counts.forEach { (k, amount) ->
                redisHashTemplate.increment(key, MetricsDailyKey.field(k.productId, k.type), amount)
            }
            redisHashTemplate.setTtlIfAbsent(key, Duration.ofDays(rankingProperties.ttlDays))
            log.debug("상품 카운트 flush [fieldCount={}]", counts.size)
        } catch (e: Exception) {
            log.error("상품 카운트 flush 실패 [fieldCount={}]", counts.size, e)
        }
    }
}
