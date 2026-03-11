package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.catalog.ProductCache
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.like.UnlikeProductCommand
import com.loopers.domain.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUnlikeProductUseCase(
    private val userService: UserService,
    private val productService: ProductService,
    private val productLikeService: ProductLikeService,
    private val productCache: ProductCache,
) : UseCase<UnlikeProductCriteria, Unit> {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun execute(criteria: UnlikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productLikeService.unlike(UnlikeProductCommand(userId = user.id, productId = criteria.productId))
        if (!productService.decreaseLikeCount(criteria.productId)) {
            log.warn("좋아요 카운트 감소 실패 (이미 0): productId={}", criteria.productId)
        }
        productCache.evictProduct(criteria.productId)
        productCache.evictPopularList()
    }
}
