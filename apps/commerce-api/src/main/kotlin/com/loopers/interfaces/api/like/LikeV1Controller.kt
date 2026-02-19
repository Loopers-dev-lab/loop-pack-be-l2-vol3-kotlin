package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.auth.AuthUser
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/likes")
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {

    @PostMapping("/{productId}")
    override fun addLike(
        @AuthUser userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.addLike(userId, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/{productId}")
    override fun removeLike(
        @AuthUser userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.removeLike(userId, productId)
        return ApiResponse.success()
    }

    @GetMapping
    override fun getLikes(
        @AuthUser userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>> {
        return likeFacade.getLikes(userId)
            .map { LikeV1Dto.LikeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
