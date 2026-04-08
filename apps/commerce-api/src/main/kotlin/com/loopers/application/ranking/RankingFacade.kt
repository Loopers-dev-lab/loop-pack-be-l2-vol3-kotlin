package com.loopers.application.ranking

import com.loopers.domain.catalog.brand.BrandRepository
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.ranking.RankingKeyPolicy
import com.loopers.domain.ranking.RankingQueryRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 랭킹 조회 Application Facade.
 *
 * 책임:
 *  - 입력 검증 (date 포맷, page/size 범위)
 *  - ZSET 조회 → 상품 정보 Aggregation (N+1 방지)
 *  - 단건 상품의 현재 순위 조회
 *
 * Aggregation 전략:
 *  - ZREVRANGE 로 productId 페이지를 받은 뒤,
 *    productRepository.findAllByIds(...) 로 단일 IN 쿼리 1회만 실행
 *  - brand 도 동일 전략 (distinct brandId 모아서 단일 IN 쿼리)
 *  - DB 조회 결과를 Map 으로 만들어 ZSET 순서를 보존하며 zip
 */
@Service
class RankingFacade(
    private val rankingQueryRepository: RankingQueryRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * 일간 랭킹 페이지 조회.
     *
     * @param dateString yyyyMMdd 포맷. null 이면 오늘.
     * @param page 1-based 페이지 번호 (최소 1)
     * @param size 페이지 크기 (1 ~ 100)
     */
    fun getRankingPage(dateString: String?, page: Int, size: Int): RankingPageResult {
        val date = parseDate(dateString)
        validatePageParams(page, size)

        val key = RankingKeyPolicy.dailyKey(date)
        val offset = ((page - 1).toLong()) * size.toLong()

        val totalCount = rankingQueryRepository.count(key)
        val entries = rankingQueryRepository.findTopN(key, offset, size.toLong())
        if (entries.isEmpty()) {
            return RankingPageResult(
                date = date.format(DATE_FORMATTER),
                page = page,
                size = size,
                totalCount = totalCount,
                items = emptyList(),
            )
        }

        // N+1 방지: 단일 IN 쿼리로 product 일괄 조회
        val productIds = entries.map { it.productId }
        val productMap = productRepository.findAllByIds(productIds).associateBy { it.id }
        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        // ZSET 순서 보존하며 zip
        val items = entries.mapIndexedNotNull { index, entry ->
            val product = productMap[entry.productId] ?: return@mapIndexedNotNull null // 삭제된 상품 skip
            val brand = brandMap[product.brandId]
            RankingItemResult(
                rank = offset + index, // 0-based 전역 순위
                score = entry.score,
                productId = product.id,
                name = product.name,
                price = product.price,
                likeCount = product.likeCount,
                brandId = product.brandId,
                brandName = brand?.name ?: "",
            )
        }

        return RankingPageResult(
            date = date.format(DATE_FORMATTER),
            page = page,
            size = size,
            totalCount = totalCount,
            items = items,
        )
    }

    /**
     * 특정 상품의 오늘 랭킹 순위 (0-based, 없으면 null).
     *
     * 상품 상세 응답에 함께 노출되어 매 호출마다 Redis 1회 round-trip 이 발생한다.
     * 향후 짧은 캐시(예: 5초 TTL) 를 둘 수 있으나 이번 주차 범위 밖.
     */
    fun findTodayRank(productId: Long): Long? {
        val key = RankingKeyPolicy.dailyKey(LocalDate.now())
        return rankingQueryRepository.findRank(key, productId)
    }

    private fun parseDate(dateString: String?): LocalDate {
        if (dateString.isNullOrBlank()) return LocalDate.now()
        return try {
            LocalDate.parse(dateString, DATE_FORMATTER)
        } catch (ex: DateTimeParseException) {
            throw CoreException(
                errorType = ErrorType.BAD_REQUEST,
                customMessage = "[$dateString] date 는 yyyyMMdd 형식이어야 합니다.",
            )
        }
    }

    private fun validatePageParams(page: Int, size: Int) {
        if (page < 1) {
            throw CoreException(
                errorType = ErrorType.BAD_REQUEST,
                customMessage = "[$page] page 는 1 이상이어야 합니다.",
            )
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw CoreException(
                errorType = ErrorType.BAD_REQUEST,
                customMessage = "[$size] size 는 1 이상 $MAX_PAGE_SIZE 이하여야 합니다.",
            )
        }
    }
}
