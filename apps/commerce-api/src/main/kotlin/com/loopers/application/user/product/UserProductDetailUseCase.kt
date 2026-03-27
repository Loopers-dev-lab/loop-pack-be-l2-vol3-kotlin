package com.loopers.application.user.product

import com.loopers.domain.product.ProductQueryRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductDetailUseCase(
    private val productQueryRepository: ProductQueryRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional(readOnly = true)
    fun getDetail(productId: Long): UserProductResult.Detail {
        val detail = UserProductResult.Detail.from(productQueryRepository.getDetail(productId))
        eventPublisher.publishEvent(ProductDetailViewedEvent(productId))
        return detail
    }
}
