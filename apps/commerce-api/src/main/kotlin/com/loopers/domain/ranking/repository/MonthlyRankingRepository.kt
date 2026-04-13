package com.loopers.domain.ranking.repository

import com.loopers.domain.ranking.model.MonthlyProductRank

interface MonthlyRankingRepository {

    /**
     * 지정 periodKey의 월간 랭킹을 rank_no 오름차순으로 조회한다.
     * 존재하지 않는 periodKey는 빈 목록을 반환한다.
     *
     * @param periodKey YYYY-MM 포맷의 월 식별자
     * @return rank_no 오름차순 최대 100건
     */
    fun findAllByPeriodKey(periodKey: String): List<MonthlyProductRank>
}
