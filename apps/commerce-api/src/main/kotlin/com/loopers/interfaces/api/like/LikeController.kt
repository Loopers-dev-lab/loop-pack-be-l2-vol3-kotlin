package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.support.auth.AuthenticatedUserInfo
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class LikeController(
    private val likeFacade: LikeFacade,
) : LikeApiSpec {

    @PostMapping("/{productId}/likes")
    override fun like(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.like(userInfo.id, productId)
        return ApiResponse.success(Unit)
    }

    @DeleteMapping("/{productId}/likes")
    override fun unlike(
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.unlike(userInfo.id, productId)
        return ApiResponse.success(Unit)
    }
}
