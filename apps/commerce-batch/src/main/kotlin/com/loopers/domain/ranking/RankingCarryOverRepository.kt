package com.loopers.domain.ranking

interface RankingCarryOverRepository {
    /**
     * sourceKey의 모든 멤버를 carryOverWeight를 곱한 점수로 destKey에 복사한다.
     * destKey가 이미 존재하면 기존 점수에 합산된다.
     *
     * @return 복사된 멤버 수
     */
    fun carryOver(sourceKey: String, destKey: String, carryOverWeight: Double): Long
}
