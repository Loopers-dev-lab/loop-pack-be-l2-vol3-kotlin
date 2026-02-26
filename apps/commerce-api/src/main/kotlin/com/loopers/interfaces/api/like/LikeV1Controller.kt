package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeService
import com.loopers.application.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val userService: UserService,
    private val likeService: LikeService,
) : LikeV1ApiSpec {

    @PostMapping("/api/v1/products/{productId}/likes")
    override fun addLike(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        val authUser = userService.authenticate(loginId, password)
        likeService.addLike(authUser.id, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun cancelLike(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        val authUser = userService.authenticate(loginId, password)
        likeService.cancelLike(authUser.id, productId)
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    override fun getUserLikes(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable userId: Long,
    ): ApiResponse<List<LikeV1Dto.LikeResponse>> {
        val authUser = userService.authenticate(loginId, password)

        if (authUser.id != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 좋아요 목록만 조회할 수 있습니다.")
        }

        return likeService.getUserLikes(userId)
            .map { LikeV1Dto.LikeResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
