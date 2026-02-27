package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserUnlikeProductUseCase(
    private val userService: UserService,
    private val productLikeRepository: ProductLikeRepository,
) : UseCase<UnlikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: UnlikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)

        val like = productLikeRepository.findByUserIdAndProductId(user.id, criteria.productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "좋아요를 찾을 수 없습니다.")

        productLikeRepository.delete(like)
    }
}
