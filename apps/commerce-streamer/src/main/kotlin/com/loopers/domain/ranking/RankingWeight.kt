package com.loopers.domain.ranking

enum class RankingWeight(val weight: Double) {
    VIEW(0.1),
    LIKE(0.2),
    ORDER(0.6),
}
