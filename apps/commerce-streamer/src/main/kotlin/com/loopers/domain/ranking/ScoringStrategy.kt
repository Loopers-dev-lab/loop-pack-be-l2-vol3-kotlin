package com.loopers.domain.ranking

interface ScoringStrategy {
    fun viewScore(): Double
    fun likeScore(): Double
    fun orderScorePerUnit(): Double
}
