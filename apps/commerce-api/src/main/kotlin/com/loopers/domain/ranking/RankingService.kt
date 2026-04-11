package com.loopers.domain.ranking

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 랭킹 조회 서비스.
 *
 * Redis ZSET에서 랭킹 데이터를 조회한다.
 * Redis 장애 시 빈 결과 / null을 반환하여 API가 정상 동작하도록 한다.
 *
 * 일간/시간 윈도우를 모두 지원한다.
 */
@Component
class RankingService(
    private val rankingRepository: RankingRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(RankingService::class.java)
    }

    /**
     * Top-N 랭킹 페이지 조회.
     *
     * @param window DAILY (yyyyMMdd) or HOURLY (yyyyMMddHH)
     * @param windowKey 윈도우 키 문자열
     * @param page 0-based 페이지 번호
     * @param size 페이지 크기
     */
    fun getTopRankings(
        window: RankingWindow,
        windowKey: String,
        page: Int,
        size: Int,
    ): RankingPageInfo {
        return try {
            val offset = (page.toLong() * size)
            val entries = rankingRepository.getTopRankings(window, windowKey, offset, size.toLong())
            val totalCount = rankingRepository.getTotalCount(window, windowKey)

            val rankings = entries.mapIndexed { index, entry ->
                RankingInfo(
                    productId = entry.productId,
                    rank = offset + index + 1,
                    score = entry.score,
                )
            }

            RankingPageInfo(
                rankings = rankings,
                totalCount = totalCount,
                page = page,
                size = size,
            )
        } catch (e: Exception) {
            log.error("[랭킹] Top-N 조회 실패 [window={}, key={}]", window, windowKey, e)
            RankingPageInfo.empty(page, size)
        }
    }

    /**
     * 개별 상품의 순위 조회 (일간 기준).
     * 랭킹에 없으면 rank=null, score=null.
     */
    fun getProductRank(dateKey: String, productId: Long): ProductRankInfo {
        return try {
            val rank = rankingRepository.getRank(RankingWindow.DAILY, dateKey, productId)
            val score = rankingRepository.getScore(RankingWindow.DAILY, dateKey, productId)

            ProductRankInfo(
                rank = rank?.let { it + 1 },
                score = score,
            )
        } catch (e: Exception) {
            log.error("[랭킹] 상품 순위 조회 실패 [dateKey={}, productId={}]", dateKey, productId, e)
            ProductRankInfo(rank = null, score = null)
        }
    }
}
