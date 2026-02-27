package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.HttpHeaders
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {

    @PostMapping("/api/v1/products/{productId}/likes")
    override fun likeProduct(
        @RequestHeader(HttpHeaders.LOGIN_ID) loginId: String,
        @RequestHeader(HttpHeaders.LOGIN_PW) loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.like(loginId, loginPw, productId)
        return ApiResponse.success(Unit)
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun unlikeProduct(
        @RequestHeader(HttpHeaders.LOGIN_ID) loginId: String,
        @RequestHeader(HttpHeaders.LOGIN_PW) loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.unlike(loginId, loginPw, productId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    override fun getLikedProducts(
        @RequestHeader(HttpHeaders.LOGIN_ID) loginId: String,
        @RequestHeader(HttpHeaders.LOGIN_PW) loginPw: String,
        @PathVariable userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikedProductResponse>> {
        return likeFacade.getLikedProducts(loginId, loginPw, userId)
            .map { LikeV1Dto.LikedProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
