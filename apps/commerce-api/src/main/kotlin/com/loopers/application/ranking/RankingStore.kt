package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEntry

interface RankingStore {
    fun getTopProducts(key: String, offset: Long, count: Long): List<RankingEntry>
    fun getTotalCount(key: String): Long
    fun getRank(key: String, productId: Long): Long?
    fun getScore(key: String, productId: Long): Double?
}
