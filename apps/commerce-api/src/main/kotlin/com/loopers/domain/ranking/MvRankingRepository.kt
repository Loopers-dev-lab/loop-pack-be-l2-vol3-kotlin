package com.loopers.domain.ranking

interface MvRankingRepository {
    fun findTop(periodKey: String, page: Int, size: Int): List<MvRankingEntry>
    fun count(periodKey: String): Long
}
