package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankedProduct
import org.springframework.stereotype.Component

/**
 * MV Entity → 도메인 [RankedProduct] 투영.
 *
 * Weekly/Monthly 모두 동일한 투영 규칙(rankNumber → rank, totalScore → score)을 적용하므로
 * 하나의 Mapper 컴포넌트로 관리한다.
 */
@Component
class RankedProductMapper {
    fun toDomain(entity: WeeklyProductRankEntity): RankedProduct =
        RankedProduct(
            productId = entity.productId,
            score = entity.totalScore,
            rank = entity.rankNumber.toLong(),
        )

    fun toDomain(entity: MonthlyProductRankEntity): RankedProduct =
        RankedProduct(
            productId = entity.productId,
            score = entity.totalScore,
            rank = entity.rankNumber.toLong(),
        )
}
