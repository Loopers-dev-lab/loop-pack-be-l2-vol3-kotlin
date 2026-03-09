package com.loopers.application.like

import com.loopers.application.UseCase
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
) : UseCase<LikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: LikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productService.getProduct(criteria.productId)
        productLikeService.like(LikeProductCommand(userId = user.id, productId = criteria.productId))
    }
}
