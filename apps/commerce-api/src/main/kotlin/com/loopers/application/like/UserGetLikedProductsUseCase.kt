package com.loopers.application.like

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.application.catalog.UserGetProductResult
import com.loopers.domain.catalog.BrandRepository
import com.loopers.domain.catalog.ProductInfo
import com.loopers.domain.catalog.ProductRepository
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetLikedProductsUseCase(
    private val userService: UserService,
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) : UseCase<GetLikedProductsCriteria, LikedProductsResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: GetLikedProductsCriteria): LikedProductsResult {
        val user = userService.getUser(criteria.loginId)

        if (user.id != criteria.userId) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 좋아요 목록만 조회할 수 있습니다.")
        }

        val pageable = PageRequest.of(criteria.page, criteria.size)
        val likeSlice = productLikeRepository.findAllByUserId(user.id, pageable)

        val sliceResult = SliceResult.from(likeSlice) { like ->
            val product = productRepository.findById(like.productId)
            val brand = product?.let { brandRepository.findById(it.brandId) }
            val info = product?.let { ProductInfo.from(it) }
            info?.let { UserGetProductResult.from(it, brandName = brand?.name ?: "") }
                ?: UserGetProductResult(
                    id = like.productId,
                    brandId = 0,
                    brandName = "",
                    name = "",
                    price = java.math.BigDecimal.ZERO,
                )
        }

        return LikedProductsResult.from(sliceResult)
    }
}
