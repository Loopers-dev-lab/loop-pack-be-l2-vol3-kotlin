package com.loopers.domain.ranking

class DefaultScoringStrategy : ScoringStrategy {
    override fun viewScore(): Double = 0.1
    override fun likeScore(): Double = 0.2
    override fun orderScorePerUnit(): Double = 0.7
}
