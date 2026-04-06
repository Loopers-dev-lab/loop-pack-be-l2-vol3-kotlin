package com.loopers.application.ranking

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.repository.RankingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Component
class GetRankingUseCase(
    private val rankingRepository: RankingRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional(readOnly = true)
    fun execute(date: LocalDate?, page: Int, size: Int): PageResult<RankingInfo> {
        val targetDate = date ?: LocalDate.now(ZoneId.of("Asia/Seoul"))
        val offset = page * size

        val totalCount = rankingRepository.getTotalCount(targetDate)
        val entries = rankingRepository.getTopN(targetDate, offset, size)
            .filter { it.score > 0 }

        if (entries.isEmpty()) {
            return PageResult(content = emptyList(), totalElements = totalCount, page = page, size = size)
        }

        val productIds = entries.map { ProductId(it.productId) }
        val productMap = productRepository.findAllByIds(productIds)
            .filter { !it.isDeleted() && it.isActive() }
            .associateBy { it.id.value }

        val rankings = entries.mapIndexedNotNull { index, entry ->
            val product = productMap[entry.productId] ?: return@mapIndexedNotNull null
            RankingInfo(
                rank = offset + index + 1,
                productId = entry.productId,
                productName = product.name,
                price = product.price.value,
                score = entry.score,
            )
        }

        return PageResult(
            content = rankings,
            totalElements = totalCount,
            page = page,
            size = size,
        )
    }
}
