package com.loopers.application.user.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.MonthlyRankQueryRepository
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.domain.ranking.RankedProduct
import com.loopers.domain.ranking.WeeklyRankQueryRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserRankingListUseCase(
    private val productRankingQueryRepository: ProductRankingQueryRepository,
    private val weeklyRankQueryRepository: WeeklyRankQueryRepository,
    private val monthlyRankQueryRepository: MonthlyRankQueryRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getList(
        date: LocalDate,
        period: RankingPeriod,
        pageRequest: PageRequest,
    ): PageResponse<UserRankingResult.RankedProduct> {
        val source = resolveSource(period)
        val offset = pageRequest.page.toLong() * pageRequest.size
        val rankedProducts = source.fetchTop(date, offset, pageRequest.size.toLong())

        if (rankedProducts.isEmpty()) {
            val totalCount = source.fetchTotal(date)
            return PageResponse(emptyList(), totalCount, pageRequest.page, pageRequest.size)
        }

        val productIds = rankedProducts.map { it.productId }
        val products = productRepository.findAllByIdIn(productIds)
            .filter { it.status == Product.Status.ACTIVE }
            .associateBy { it.id!! }

        val brandIds = products.values.map { it.brandId }.distinct()
        val activeBrands = brandRepository.findAllByIdIn(brandIds)
            .filter { it.status == Brand.Status.ACTIVE }
            .associateBy { it.id!! }

        val totalCount = source.fetchTotal(date)

        val content = rankedProducts
            .filter { products.containsKey(it.productId) }
            .filter { activeBrands.containsKey(products[it.productId]!!.brandId) }
            .map { ranked ->
                val product = products[ranked.productId]!!
                UserRankingResult.RankedProduct(
                    rank = ranked.rank,
                    score = ranked.score,
                    productId = product.id!!,
                    productName = product.name,
                    sellingPrice = product.sellingPrice.amount,
                    thumbnailUrl = product.thumbnailUrl,
                )
            }

        return PageResponse(content, totalCount, pageRequest.page, pageRequest.size)
    }

    /**
     * period에 따라 단일 source를 한 번 고르고 fetchTop/fetchTotal에 동일하게 적용한다.
     * 두 호출에 서로 다른 repository가 쓰이는 drift를 구조적으로 막는다.
     */
    private fun resolveSource(period: RankingPeriod): RankSource =
        when (period) {
            RankingPeriod.DAILY -> RankSource(
                fetchTop = productRankingQueryRepository::getTopRanked,
                fetchTotal = productRankingQueryRepository::getTotalCount,
            )
            RankingPeriod.WEEKLY -> RankSource(
                fetchTop = weeklyRankQueryRepository::getTopRanked,
                fetchTotal = weeklyRankQueryRepository::getTotalCount,
            )
            RankingPeriod.MONTHLY -> RankSource(
                fetchTop = monthlyRankQueryRepository::getTopRanked,
                fetchTotal = monthlyRankQueryRepository::getTotalCount,
            )
        }

    private data class RankSource(
        val fetchTop: (LocalDate, Long, Long) -> List<RankedProduct>,
        val fetchTotal: (LocalDate) -> Long,
    )
}
