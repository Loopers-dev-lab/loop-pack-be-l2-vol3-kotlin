package com.loopers.interfaces.api.v1.like

import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.GetMyLikesUseCase
import com.loopers.application.like.RemoveLikeUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.AuthUser
import com.loopers.interfaces.api.auth.AuthenticatedUser
import com.loopers.interfaces.api.common.PageResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeController(
    private val addLikeUseCase: AddLikeUseCase,
    private val removeLikeUseCase: RemoveLikeUseCase,
    private val getMyLikesUseCase: GetMyLikesUseCase,
) {
    @PostMapping("/api/v1/products/{productId}/likes")
    fun addLike(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable productId: Long,
    ): ApiResponse<Nothing?> {
        addLikeUseCase.add(authUser.id, productId)
        return ApiResponse.success(null)
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    fun removeLike(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable productId: Long,
    ): ApiResponse<Nothing?> {
        removeLikeUseCase.remove(authUser.id, productId)
        return ApiResponse.success(null)
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    fun getMyLikes(
        @AuthenticatedUser authUser: AuthUser,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResponse<GetMyLikeResponse>> {
        if (authUser.id != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 좋아요 목록만 조회할 수 있습니다.")
        }
        val result = getMyLikesUseCase.getMyLikes(authUser.id, page, size)
        return ApiResponse.success(PageResponse.from(result) { GetMyLikeResponse.from(it) })
    }
}
