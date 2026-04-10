package com.loopers.application.ranking

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.repository.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Component
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {

    private val readOnlyTxTemplate = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    private val totalCountCache = ConcurrentHashMap<LocalDate, TotalCountSnapshot>()

    private data class TotalCountSnapshot(
        val count: Long,
        val expiresAt: Instant,
    )

    fun execute(date: LocalDate?, page: Int, size: Int): PageResult<RankingInfo> {
        val targetDate = date ?: LocalDate.now(clock)
        val rankings = fetchVisibleRankings(targetDate, page, size)
        val totalElements = computeTotalVisibleCount(targetDate)

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
        val skipCount = page.toLong() * size
        val result = mutableListOf<RankingInfo>()
        var redisOffset = 0
        var visibleSeen = 0L

        while (result.size < size) {
            val (entries, rawFetchCount) = rankingRepository.getTopN(date, redisOffset, FETCH_BATCH_SIZE)
            if (rawFetchCount == 0) break

            val productIds = entries.map { ProductId(it.productId) }
            val productMap = checkNotNull(
                readOnlyTxTemplate.execute {
                    productRepository.findAllByIds(productIds)
                        .filter { it.isActive() }
                        .associateBy { it.id.value }
                },
            ) { "readOnlyTxTemplate.execute returned null at fetchVisibleRankings" }

            for (entry in entries) {
                val product = productMap[entry.productId] ?: continue
                if (visibleSeen < skipCount) {
                    visibleSeen++
                    continue
                }
                if (result.size >= size) break
                result.add(
                    RankingInfo(
                        rank = (skipCount + result.size + 1).toInt(),
                        productId = entry.productId,
                        productName = product.name,
                        price = product.price.value,
                        score = entry.score,
                    ),
                )
            }

            redisOffset += rawFetchCount
            if (rawFetchCount < FETCH_BATCH_SIZE) break
        }

        return result
    }

    /**
     * 캐시가 유효하면(같은 date + 만료 전) 캐시 값을 반환하고,
     * 그렇지 않으면 전체 스캔 후 캐시를 갱신한다.
     *
     * `ConcurrentHashMap.compute`로 동일 date 키에 대한 read-then-write 구간을 원자화하여,
     * 캐시 만료 직후 동시 요청이 `scanTotalVisibleCount` 를 중복 호출하지 않도록 보장한다.
     */
    private fun computeTotalVisibleCount(date: LocalDate): Long {
        val now = Instant.now(clock)
        // opportunistic cleanup: 만료된 다른 날짜 엔트리 정리 (현재 compute 바깥이라 레이스 무해)
        totalCountCache.entries.removeIf { it.value.expiresAt.isBefore(now) }
        val snapshot = checkNotNull(
            totalCountCache.compute(date) { _, cached ->
                if (cached != null && cached.expiresAt.isAfter(now)) {
                    cached
                } else {
                    TotalCountSnapshot(
                        count = scanTotalVisibleCount(date),
                        expiresAt = now.plusSeconds(TOTAL_COUNT_CACHE_TTL_SECONDS),
                    )
                }
            },
        ) { "totalCountCache.compute returned null at computeTotalVisibleCount" }
        return snapshot.count
    }

    /**
     * 전체 score > 0 항목 중 active 상품 건수를 배치 반복으로 정확히 산출한다.
     * score > 0 필터는 Repository(Redis) 레벨에서 적용된다.
     */
    private fun scanTotalVisibleCount(date: LocalDate): Long {
        var offset = 0
        var totalActive = 0L

        while (true) {
            val (entries, rawFetchCount) = rankingRepository.getTopN(date, offset, FETCH_BATCH_SIZE)
            if (rawFetchCount == 0) break

            val productIds = entries.map { ProductId(it.productId) }
            totalActive += checkNotNull(
                readOnlyTxTemplate.execute {
                    productRepository.findAllByIds(productIds).count { it.isActive() }.toLong()
                },
            ) { "readOnlyTxTemplate.execute returned null at scanTotalVisibleCount" }

            offset += rawFetchCount
            if (rawFetchCount < FETCH_BATCH_SIZE) break
        }

        return totalActive
    }

    companion object {
        private const val FETCH_BATCH_SIZE = 500
        private const val TOTAL_COUNT_CACHE_TTL_SECONDS = 30L
    }
}
