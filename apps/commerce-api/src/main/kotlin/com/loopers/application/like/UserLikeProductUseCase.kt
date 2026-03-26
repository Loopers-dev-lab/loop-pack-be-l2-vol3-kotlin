package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.common.event.ProductLikedEvent
import com.loopers.domain.like.LikeProductCommand
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductMetricsRedisRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserLikeProductUseCase(
    private val userService: UserService,
    private val productService: ProductService,
    private val productLikeService: ProductLikeService,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : UseCase<LikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: LikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productService.getProduct(criteria.productId)
        val created = productLikeService.like(LikeProductCommand(userId = user.id, productId = criteria.productId))

        if (created) {
            productMetricsRedisRepository.incrementLikeCount(criteria.productId)
        }

        eventPublisher.publishEvent(
            ProductLikedEvent(
                userId = user.id,
                loginId = criteria.loginId,
                productId = criteria.productId,
                isNewLike = created,
            ),
        )
    }
}
