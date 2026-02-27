package com.loopers.application.like

import com.loopers.application.UseCase
import com.loopers.domain.catalog.ProductRepository
import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserLikeProductUseCase(
    private val userService: UserService,
    private val productRepository: ProductRepository,
    private val productLikeRepository: ProductLikeRepository,
) : UseCase<LikeProductCriteria, Unit> {

    @Transactional
    override fun execute(criteria: LikeProductCriteria) {
        val user = userService.getUser(criteria.loginId)

        productRepository.findById(criteria.productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")

        productLikeRepository.findByUserIdAndProductId(user.id, criteria.productId)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다.")
        }

        val like = ProductLikeModel(userId = user.id, productId = criteria.productId)
        productLikeRepository.save(like)
    }
}
