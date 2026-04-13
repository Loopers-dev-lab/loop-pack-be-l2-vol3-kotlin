package com.loopers.domain.ranking.repository

import com.loopers.domain.ranking.model.WeeklyProductRank

interface WeeklyRankingRepository {

    /**
     * 지정 periodKey의 주간 랭킹을 rank_no 오름차순으로 조회한다.
     * 존재하지 않는 periodKey는 빈 목록을 반환한다.
     *
     * @param periodKey YYYY-Www 포맷의 ISO 주 식별자
     * @return rank_no 오름차순 최대 100건
     */
    fun findAllByPeriodKey(periodKey: String): List<WeeklyProductRank>
}
