package com.loopers.batch.job.ranking

import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "ranking.weight")
data class RankingWeightProperties(
    @field:PositiveOrZero
    val viewWeight: Double = 0.1,

    @field:PositiveOrZero
    val likeWeight: Double = 0.2,

    @field:PositiveOrZero
    val salesWeight: Double = 0.7,
) {
    init {
        // 개별 필드 @PositiveOrZero로 음수는 차단되지만, 모두 0인 경우는 score가 항상 0이 되어 랭킹이 무의미해지므로 별도 검증한다
        require(viewWeight + likeWeight + salesWeight > 0.0) {
            "가중치 합은 0보다 커야 합니다. (view=$viewWeight, like=$likeWeight, sales=$salesWeight)"
        }
    }
}
