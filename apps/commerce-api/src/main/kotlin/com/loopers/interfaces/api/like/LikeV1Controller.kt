package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.interfaces.api.ApiResponse
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
    override fun like(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.like(loginId, password, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun unlike(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.unlike(loginId, password, productId)
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/likes")
    override fun getUserLikes(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<LikeV1Dto.LikeProductResponse>> {
        return likeFacade.getUserLikes(loginId, password)
            .map { LikeV1Dto.LikeProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
