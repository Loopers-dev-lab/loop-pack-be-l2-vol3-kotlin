package com.loopers.interfaces.api.like

import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.GetUserLikesUseCase
import com.loopers.application.like.RemoveLikeUseCase
import com.loopers.interfaces.api.like.dto.LikeV1Dto
import com.loopers.interfaces.api.like.spec.LikeV1ApiSpec
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val addLikeUseCase: AddLikeUseCase,
    private val removeLikeUseCase: RemoveLikeUseCase,
    private val getUserLikesUseCase: GetUserLikesUseCase,
) : LikeV1ApiSpec {

    @PostMapping("/api/v1/products/{productId}/likes")
    override fun addLike(
        @AuthUser userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        addLikeUseCase.execute(userId, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun removeLike(
        @AuthUser userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        removeLikeUseCase.execute(userId, productId)
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/users/likes")
    override fun getLikes(
        @AuthUser userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>> {
        return getUserLikesUseCase.execute(userId)
            .map { info -> LikeV1Dto.LikeResponse.from(info) }
            .let { ApiResponse.success(it) }
    }
}
