package com.loopers.interfaces.api.ranking

import com.loopers.domain.ranking.RankingWeights

class RankingAdminV1Dto {

    data class UpdateWeightsRequest(
        val view: Double,
        val like: Double,
        val order: Double,
    ) {
        fun toWeights(): RankingWeights {
            return RankingWeights(
                view = view,
                like = like,
                order = order,
            )
        }
    }

    data class WeightsResponse(
        val view: Double,
        val like: Double,
        val order: Double,
    ) {
        companion object {
            fun from(weights: RankingWeights): WeightsResponse {
                return WeightsResponse(
                    view = weights.view,
                    like = weights.like,
                    order = weights.order,
                )
            }
        }
    }
}
