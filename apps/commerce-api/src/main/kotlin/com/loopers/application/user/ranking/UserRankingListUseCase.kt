package com.loopers.application.user.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UserRankingListUseCase(
    private val productRankingQueryRepository: ProductRankingQueryRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional(readOnly = true)
    fun getList(
        date: LocalDate,
        pageRequest: PageRequest,
    ): PageResponse<UserRankingResult.RankedProduct> {
        val offset = pageRequest.page.toLong() * pageRequest.size
        val rankedProducts = productRankingQueryRepository.getTopRanked(date, offset, pageRequest.size.toLong())

        if (rankedProducts.isEmpty()) {
            val totalCount = productRankingQueryRepository.getTotalCount(date)
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

        val totalCount = productRankingQueryRepository.getTotalCount(date)

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
}
