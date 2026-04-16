package com.loopers.application.ranking

import com.loopers.common.DateUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GetProductRankUseCase(
    private val rankingStore: RankingStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(productId: Long): Int? {
        return try {
            rankingStore.getProductRank(DateUtils.todayKst(), productId)?.let { (it + 1).toInt() }
        } catch (e: Exception) {
            log.warn("상품 랭킹 조회 실패 [productId={}]", productId, e)
            null
        }
    }
}
