package com.loopers.application.ranking

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GetProductRankUseCase(
    private val rankingStore: RankingStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(productId: Long): Int? {
        return try {
            rankingStore.getProductRank(LocalDate.now(), productId)?.let { (it + 1).toInt() }
        } catch (e: Exception) {
            log.warn("상품 랭킹 조회 실패 [productId={}]", productId, e)
            null
        }
    }
}
