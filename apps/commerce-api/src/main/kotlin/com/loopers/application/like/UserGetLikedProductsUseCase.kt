package com.loopers.application.like

import com.loopers.application.SliceResult
import com.loopers.application.UseCase
import com.loopers.application.catalog.UserGetProductResult
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.ProductService
import com.loopers.domain.like.ProductLikeService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class UserGetLikedProductsUseCase(
    private val userService: UserService,
    private val productLikeService: ProductLikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
) : UseCase<GetLikedProductsCriteria, LikedProductsResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: GetLikedProductsCriteria): LikedProductsResult {
        val user = userService.getUser(criteria.loginId)

        if (user.id != criteria.userId) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 좋아요 목록만 조회할 수 있습니다.")
        }

        val pageable = PageRequest.of(criteria.page, criteria.size)
        val likeSlice = productLikeService.getLikedProducts(user.id, pageable)

        val sliceResult = SliceResult.from(likeSlice) { likeInfo ->
            val productInfo = productService.findProduct(likeInfo.productId)
            val brandInfo = productInfo?.let { brandService.findBrand(it.brandId) }
            productInfo?.let { UserGetProductResult.from(it, brandName = brandInfo?.name ?: "") }
                ?: UserGetProductResult(
                    id = likeInfo.productId,
                    brandId = 0,
                    brandName = "",
                    name = "",
                    price = BigDecimal.ZERO,
                )
        }

        return LikedProductsResult.from(sliceResult)
    }
}
