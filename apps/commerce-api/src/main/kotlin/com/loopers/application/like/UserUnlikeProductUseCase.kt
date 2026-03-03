package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.like.UnlikeProductCommand
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUnlikeProductUseCase(
    private val userService: UserService,
    private val productLikeService: ProductLikeService,
) : UseCase<UnlikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: UnlikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)
        productLikeService.unlike(UnlikeProductCommand(userId = user.id, productId = criteria.productId))
    }
}
