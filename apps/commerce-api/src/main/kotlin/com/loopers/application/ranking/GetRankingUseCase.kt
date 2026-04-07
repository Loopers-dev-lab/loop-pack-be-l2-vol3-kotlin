package com.loopers.application.ranking

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.repository.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Component
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional(readOnly = true)
    fun execute(date: LocalDate?, page: Int, size: Int): PageResult<RankingInfo> {
        val targetDate = date ?: LocalDate.now(ZoneId.of("Asia/Seoul"))
        val rankings = fetchVisibleRankings(targetDate, page, size)
        val totalElements = if (rankings.isEmpty() && page == 0) {
            0L
        } else {
            computeTotalVisibleCount(targetDate)
        }

        return PageResult(
            content = rankings,
            totalElements = totalElements,
            page = page,
            size = size,
        )
    }

    /**
     * 반복 조회로 visible size 충족 (sparse page 보정).
     * Redis에서 score > 0 항목을 offset부터 조회 -> DB 필터(isActive) ->
     * visible 건수가 size 미달이면 다음 청크를 이어서 조회.
     */
    private fun fetchVisibleRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val rankings = mutableListOf<RankingInfo>()
        var redisOffset = page * size

        while (rankings.size < size) {
            val entries = rankingRepository.getTopN(date, redisOffset, size)
            if (entries.isEmpty()) break

            val productIds = entries.map { ProductId(it.productId) }
            val productMap = productRepository.findAllByIds(productIds)
                .filter { it.isActive() }
                .associateBy { it.id.value }

            for (entry in entries) {
                if (rankings.size >= size) break
                val product = productMap[entry.productId] ?: continue
                rankings.add(
                    RankingInfo(
                        rank = page * size + rankings.size + 1,
                        productId = entry.productId,
                        productName = product.name,
                        price = product.price.value,
                        score = entry.score,
                    ),
                )
            }

            redisOffset += entries.size
            if (entries.size < size) break
        }

        return rankings
    }

    /**
     * 전체 score > 0 항목 중 active 상품 건수를 산출한다.
     * score > 0 필터는 Repository(Redis) 레벨에서 적용된다.
     */
    private fun computeTotalVisibleCount(date: LocalDate): Long {
        val allEntries = rankingRepository.getTopN(date, 0, MAX_RANKING_ENTRIES)
        if (allEntries.isEmpty()) return 0L
        val allProductIds = allEntries.map { ProductId(it.productId) }
        return productRepository.findAllByIds(allProductIds).count { it.isActive() }.toLong()
    }

    companion object {
        private const val MAX_RANKING_ENTRIES = 10_000
    }
}
