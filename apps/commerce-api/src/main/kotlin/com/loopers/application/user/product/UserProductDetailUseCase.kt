package com.loopers.application.user.product

import com.loopers.domain.product.ProductQueryRepository
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.support.event.user.ProductDetailViewedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class UserProductDetailUseCase(
    private val productQueryRepository: ProductQueryRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val productRankingQueryRepository: ProductRankingQueryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getDetail(productId: Long): UserProductResult.Detail {
        val detail = UserProductResult.Detail.from(productQueryRepository.getDetail(productId))
        eventPublisher.publishEvent(ProductDetailViewedEvent(productId))
        val rank = fetchRank(productId)
        return detail.copy(rank = rank)
    }

    private fun fetchRank(productId: Long): Long? =
        runCatching {
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            productRankingQueryRepository.getRank(today, productId)
        }.onFailure { e ->
            log.warn("Failed to fetch ranking for productId={}", productId, e)
        }.getOrNull()
}
