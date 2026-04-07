package com.loopers.application.ranking

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.repository.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Component
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun execute(date: LocalDate?, page: Int, size: Int): PageResult<RankingInfo> {
        val targetDate = date ?: LocalDate.now(clock)
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
     * offset 0부터 스캔하며 visible 항목을 수집하는 방식으로 페이지 경계 정합성을 보장한다.
     * 앞 페이지에서 DB 필터로 제외된 항목 수에 관계없이 뒤 페이지가 정확히 이어진다.
     */
    private fun fetchVisibleRankings(date: LocalDate, page: Int, size: Int): List<RankingInfo> {
        val skipCount = page * size
        val result = mutableListOf<RankingInfo>()
        var redisOffset = 0
        var visibleSeen = 0

        while (result.size < size) {
            val entries = rankingRepository.getTopN(date, redisOffset, FETCH_BATCH_SIZE)
            if (entries.isEmpty()) break

            val productIds = entries.map { ProductId(it.productId) }
            val productMap = productRepository.findAllByIds(productIds)
                .filter { it.isActive() }
                .associateBy { it.id.value }

            for (entry in entries) {
                val product = productMap[entry.productId] ?: continue
                if (visibleSeen < skipCount) {
                    visibleSeen++
                    continue
                }
                if (result.size >= size) break
                result.add(
                    RankingInfo(
                        rank = skipCount + result.size + 1,
                        productId = entry.productId,
                        productName = product.name,
                        price = product.price.value,
                        score = entry.score,
                    ),
                )
            }

            redisOffset += entries.size
            if (entries.size < FETCH_BATCH_SIZE) break
        }

        return result
    }

    /**
     * 전체 score > 0 항목 중 active 상품 건수를 배치 반복으로 정확히 산출한다.
     * score > 0 필터는 Repository(Redis) 레벨에서 적용된다.
     */
    private fun computeTotalVisibleCount(date: LocalDate): Long {
        var offset = 0
        var totalActive = 0L

        while (true) {
            val entries = rankingRepository.getTopN(date, offset, FETCH_BATCH_SIZE)
            if (entries.isEmpty()) break

            val productIds = entries.map { ProductId(it.productId) }
            totalActive += productRepository.findAllByIds(productIds).count { it.isActive() }

            offset += entries.size
            if (entries.size < FETCH_BATCH_SIZE) break
        }

        return totalActive
    }

    companion object {
        private const val FETCH_BATCH_SIZE = 500
    }
}
