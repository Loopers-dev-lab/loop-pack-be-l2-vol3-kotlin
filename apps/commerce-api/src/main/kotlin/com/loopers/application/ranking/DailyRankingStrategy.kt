package com.loopers.application.ranking

import com.loopers.application.product.ProductCacheManager
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component(DailyRankingStrategy.BEAN_NAME)
class DailyRankingStrategy(
    private val rankingService: RankingService,
    private val productCacheManager: ProductCacheManager,
) : RankingStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val BEAN_NAME = "dailyRankingStrategy"
    }

    override fun getRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val entries = runCatching {
            rankingService.getTopRankings(date, page, size)
        }.getOrElse {
            log.warn("[DailyRankingStrategy] Redis 랭킹 조회 실패, DB fallback 수행", it)
            rankingService.getTopRankingsFromDb(page, size)
        }
        return toRankingInfoList(entries, page, size)
    }

    private fun toRankingInfoList(entries: List<RankingEntry>, page: Int, size: Int): List<RankingInfo> {
        val startRank = ((page - 1) * size).toLong()
        return entries.mapIndexed { index, entry ->
            val product = productCacheManager.getProduct(entry.productId)
            RankingInfo(
                productId = entry.productId,
                rank = startRank + index + 1,
                score = entry.score,
                productName = product.name,
                productPrice = product.price,
            )
        }
    }
}
