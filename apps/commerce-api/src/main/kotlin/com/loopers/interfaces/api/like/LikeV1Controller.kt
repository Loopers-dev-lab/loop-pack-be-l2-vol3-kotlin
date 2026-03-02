package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.CurrentUser
import com.loopers.support.auth.LoginUser
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {

    @PostMapping("/api/v1/products/{productId}/likes")
    override fun likeProduct(
        @CurrentUser loginUser: LoginUser,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.like(loginUser.id, productId)
        return ApiResponse.success(Unit)
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun unlikeProduct(
        @CurrentUser loginUser: LoginUser,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.unlike(loginUser.id, productId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/api/v1/users/me/likes")
    override fun getLikedProducts(
        @CurrentUser loginUser: LoginUser,
    ): ApiResponse<List<LikeV1Dto.LikedProductResponse>> {
        return likeFacade.getLikedProducts(loginUser.id)
            .map { LikeV1Dto.LikedProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
