package com.loopers.application.ranking

import com.loopers.application.UseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import com.loopers.infrastructure.catalog.ProductRankRedisReader
import org.springframework.stereotype.Component

@Component
class UserGetRankingUseCase(
    private val productRankRedisReader: ProductRankRedisReader,
    private val productService: ProductService,
    private val brandService: BrandService,
) : UseCase<GetRankingCriteria, GetRankingResult> {

    override fun execute(criteria: GetRankingCriteria): GetRankingResult {
        // 1) ZSET에서 페이지 단위로 (productId, score, rank) 조회
        val entries = productRankRedisReader.getRankingPage(criteria.date, criteria.page, criteria.size)
        val totalCount = productRankRedisReader.getTotalCount(criteria.date)

        if (entries.isEmpty()) {
            return GetRankingResult(
                date = criteria.date,
                page = criteria.page,
                size = criteria.size,
                totalCount = totalCount,
                hasNext = false,
                items = emptyList(),
            )
        }

        // 2) productId, brandId를 bulk로 조회 (N+1 방지)
        val productIds = entries.map { it.productId }
        val products = productService.getProductsByIds(productIds).associateBy { it.id }

        val brandIds = products.values.map { it.brandId }.distinct()
        val brands = brandService.getBrandsByIds(brandIds).associateBy { it.id }

        // 3) ZSET 순위 순서 그대로 응답 조립 — DB에 없는 productId는 결과에서 제외
        val items = entries.mapNotNull { entry ->
            val product = products[entry.productId] ?: return@mapNotNull null
            RankedProductResult(
                rank = entry.rank,
                score = entry.score,
                productId = product.id,
                name = product.name,
                price = product.price,
                brandId = product.brandId,
                brandName = brands[product.brandId]?.name ?: "",
            )
        }

        // hasNext는 ZSET entries 개수 기준으로 판단 — soft-delete로 items가 줄어도
        // 다음 페이지에 데이터가 있을 수 있으므로 entries(Redis 기준)로 판단하는 게 정확하다.
        // totalCount는 Redis ZCARD 기준 근사값 (soft-delete 상품 포함 가능).
        return GetRankingResult(
            date = criteria.date,
            page = criteria.page,
            size = criteria.size,
            totalCount = totalCount,
            hasNext = entries.size >= criteria.size,
            items = items,
        )
    }
}
