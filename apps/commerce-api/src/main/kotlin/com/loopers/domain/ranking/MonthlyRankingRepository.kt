package com.loopers.domain.ranking

import java.time.LocalDate

interface MonthlyRankingRepository {

    /**
     * 해당 월의 랭킹을 페이지 단위로 조회한다.
     * @param page 0-based 페이지 번호
     * @return RankingEntry 리스트 (rank는 0-based)
     */
    fun findRankings(rankingDate: LocalDate, page: Int, size: Int): List<RankingEntry>

    fun countByRankingDate(rankingDate: LocalDate): Long
}
