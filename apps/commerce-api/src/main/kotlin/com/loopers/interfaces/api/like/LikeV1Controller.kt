package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.config.auth.AuthenticatedMember
import com.loopers.config.auth.MemberAuthenticated
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {
    @MemberAuthenticated
    @PostMapping("/api/v1/products/{productId}/likes")
    override fun like(
        authenticatedMember: AuthenticatedMember,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.like(authenticatedMember.id, productId)
        return ApiResponse.success()
    }

    @MemberAuthenticated
    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun unlike(
        authenticatedMember: AuthenticatedMember,
        @PathVariable productId: Long,
    ): ApiResponse<Any> {
        likeFacade.unlike(authenticatedMember.id, productId)
        return ApiResponse.success()
    }

    @MemberAuthenticated
    @GetMapping("/api/v1/likes")
    override fun getMyLikes(
        authenticatedMember: AuthenticatedMember,
    ): ApiResponse<List<LikeV1Dto.LikedProductResponse>> {
        return likeFacade.getMyLikes(authenticatedMember.id)
            .map { LikeV1Dto.LikedProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
