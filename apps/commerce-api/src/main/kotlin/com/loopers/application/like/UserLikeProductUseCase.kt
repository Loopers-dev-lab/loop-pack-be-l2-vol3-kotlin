package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.catalog.ProductCache
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.like.LikeProductCommand
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserLikeProductUseCase(
    private val userService: UserService,
    private val productService: ProductService,
    private val productLikeService: ProductLikeService,
    private val productCache: ProductCache,
) : UseCase<LikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: LikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productService.getProduct(criteria.productId)
        val created = productLikeService.like(LikeProductCommand(userId = user.id, productId = criteria.productId))
        if (created) {
            productCache.evictProduct(criteria.productId)
            productCache.evictPopularList()
        }
    }
}
