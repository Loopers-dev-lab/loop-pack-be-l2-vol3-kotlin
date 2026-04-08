package com.loopers.application.product

import com.loopers.application.event.DirectEventPublisher
import com.loopers.domain.ranking.RankingService
import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import com.loopers.event.EventTypes
import com.loopers.event.Topics
import com.loopers.event.payload.ProductViewedPayload
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ProductFacade(
    private val productCacheManager: ProductCacheManager,
    private val directEventPublisher: DirectEventPublisher,
    private val eventPublisher: ApplicationEventPublisher,
    private val rankingService: RankingService,
) {

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo> {
        return productCacheManager.getProducts(brandId, pageQuery)
    }

    fun getProduct(productId: Long): ProductDetailInfo {
        val product = productCacheManager.getProduct(productId)
        directEventPublisher.publish(
            topic = Topics.CATALOG,
            key = productId.toString(),
            eventType = EventTypes.VIEWED,
            payload = ProductViewedPayload(productId),
        )
        eventPublisher.publishEvent(
            UserActionEvent(userId = 0L, actionType = ActionType.PRODUCT_VIEWED, targetId = productId),
        )
        val zeroBasedRank = rankingService.getRank(LocalDate.now(), productId)
        val rank = zeroBasedRank?.let { it + 1 }
        return product.withRank(rank)
    }
}
