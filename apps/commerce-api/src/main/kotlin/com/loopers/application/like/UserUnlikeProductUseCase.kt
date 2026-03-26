package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.common.event.ProductUnlikedEvent
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.like.UnlikeProductCommand
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductMetricsRedisRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUnlikeProductUseCase(
    private val userService: UserService,
    private val productLikeService: ProductLikeService,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : UseCase<UnlikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: UnlikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productLikeService.unlike(UnlikeProductCommand(userId = user.id, productId = criteria.productId))

        productMetricsRedisRepository.decrementLikeCount(criteria.productId)

        eventPublisher.publishEvent(
            ProductUnlikedEvent(
                userId = user.id,
                loginId = criteria.loginId,
                productId = criteria.productId,
            ),
        )
    }
}
