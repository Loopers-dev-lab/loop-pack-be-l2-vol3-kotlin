package com.loopers.interfaces.api.like

import com.loopers.application.like.GetLikedProductsCriteria
import com.loopers.application.like.LikeProductCriteria
import com.loopers.application.like.UnlikeProductCriteria
import com.loopers.application.like.UserGetLikedProductsUseCase
import com.loopers.application.like.UserLikeProductUseCase
import com.loopers.application.like.UserUnlikeProductUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductLikeV1Controller(
    private val userLikeProductUseCase: UserLikeProductUseCase,
    private val userUnlikeProductUseCase: UserUnlikeProductUseCase,
    private val userGetLikedProductsUseCase: UserGetLikedProductsUseCase,
) : ProductLikeV1ApiSpec {

    @PostMapping("/api/v1/products/{productId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    override fun likeProduct(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ) {
        userLikeProductUseCase.execute(
            LikeProductCriteria(loginId = loginId, productId = productId),
        )
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun unlikeProduct(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ) {
        userUnlikeProductUseCase.execute(
            UnlikeProductCriteria(loginId = loginId, productId = productId),
        )
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    override fun getLikedProducts(
        @RequestHeader(value = "X-Loopers-LoginId") loginId: String,
        @RequestHeader(value = "X-Loopers-LoginPw") loginPw: String,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductLikeV1Dto.LikedProductSliceResponse> {
        return userGetLikedProductsUseCase.execute(
            GetLikedProductsCriteria(loginId = loginId, userId = userId, page = page, size = size),
        )
            .let { ProductLikeV1Dto.LikedProductSliceResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
