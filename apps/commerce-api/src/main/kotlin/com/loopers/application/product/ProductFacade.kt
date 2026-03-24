package com.loopers.application.product

import com.loopers.domain.user.event.ActionType
import com.loopers.domain.user.event.UserActionEvent
import com.loopers.support.common.PageQuery
import com.loopers.support.common.PageResult
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productCacheManager: ProductCacheManager,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun getProducts(brandId: Long?, pageQuery: PageQuery): PageResult<ProductInfo> {
        return productCacheManager.getProducts(brandId, pageQuery)
    }

    fun getProduct(productId: Long): ProductDetailInfo {
        val product = productCacheManager.getProduct(productId)
        eventPublisher.publishEvent(
            UserActionEvent(userId = 0L, actionType = ActionType.PRODUCT_VIEWED, targetId = productId),
        )
        return product
    }
}
