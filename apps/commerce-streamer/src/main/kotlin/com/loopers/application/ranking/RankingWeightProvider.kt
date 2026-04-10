package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingScoreConfigRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class RankingWeightProvider(
    private val rankingScoreConfigRepository: RankingScoreConfigRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val weights = AtomicReference(Weights())

    @PostConstruct
    fun init() {
        refresh()
    }

    @Scheduled(fixedRate = REFRESH_INTERVAL_MS)
    fun refresh() {
        try {
            val configs = rankingScoreConfigRepository.findAll()
            val configMap = configs.associate { it.configKey to it.configValue }
            weights.set(
                Weights(
                    viewWeight = configMap["VIEW_WEIGHT"] ?: DEFAULT_VIEW_WEIGHT,
                    likeWeight = configMap["LIKE_WEIGHT"] ?: DEFAULT_LIKE_WEIGHT,
                    orderWeight = configMap["ORDER_WEIGHT"] ?: DEFAULT_ORDER_WEIGHT,
                    carryOverWeight = configMap["CARRY_OVER_WEIGHT"] ?: DEFAULT_CARRY_OVER_WEIGHT,
                ),
            )
        } catch (e: Exception) {
            log.warn("랭킹 가중치 DB 조회 실패, 기존 값 유지", e)
        }
    }

    fun getViewWeight(): Double = weights.get().viewWeight
    fun getLikeWeight(): Double = weights.get().likeWeight
    fun getOrderWeight(): Double = weights.get().orderWeight
    fun getCarryOverWeight(): Double = weights.get().carryOverWeight

    private data class Weights(
        val viewWeight: Double = DEFAULT_VIEW_WEIGHT,
        val likeWeight: Double = DEFAULT_LIKE_WEIGHT,
        val orderWeight: Double = DEFAULT_ORDER_WEIGHT,
        val carryOverWeight: Double = DEFAULT_CARRY_OVER_WEIGHT,
    )

    companion object {
        const val DEFAULT_VIEW_WEIGHT = 0.1
        const val DEFAULT_LIKE_WEIGHT = 0.2
        const val DEFAULT_ORDER_WEIGHT = 0.6
        const val DEFAULT_CARRY_OVER_WEIGHT = 0.1
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }
}
