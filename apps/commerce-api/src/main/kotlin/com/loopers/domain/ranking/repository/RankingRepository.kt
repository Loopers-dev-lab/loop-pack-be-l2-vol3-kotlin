package com.loopers.domain.ranking.repository

import com.loopers.domain.ranking.model.RankingEntry
import java.time.LocalDate

interface RankingRepository {

    /**
     * 지정 날짜의 랭킹을 점수 내림차순으로 조회한다.
     * score > 0인 항목만 반환한다.
     *
     * @param date 랭킹 기준 날짜 (KST)
     * @param offset 시작 위치 (0-based)
     * @param limit 조회 개수
     * @return 점수 내림차순 정렬된 랭킹 항목. 키가 없으면 빈 리스트.
     */
    fun getTopN(date: LocalDate, offset: Int, limit: Int): List<RankingEntry>

    /**
     * 특정 상품의 순위를 조회한다.
     * score <= 0인 상품은 null을 반환한다.
     *
     * @param date 랭킹 기준 날짜 (KST)
     * @param productId 상품 ID
     * @return 1-based 순위. 랭킹에 없거나 score <= 0이면 null.
     */
    fun getRank(date: LocalDate, productId: Long): Int?
}
