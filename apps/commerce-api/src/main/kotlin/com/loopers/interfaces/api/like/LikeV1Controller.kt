package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.domain.auth.AuthenticatedMember
import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeService
import com.loopers.infrastructure.auth.JwtAuthenticationFilter
import com.loopers.interfaces.api.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val likeFacade: LikeFacade,
    private val likeService: LikeService,
) {
    @PostMapping("/api/v1/products/{productId}/likes")
    fun like(
        @PathVariable productId: Long,
        request: HttpServletRequest,
    ): ApiResponse<Any> {
        val member = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        likeFacade.likeProduct(member.memberId, productId)
        return ApiResponse.success()
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    fun unlike(
        @PathVariable productId: Long,
        request: HttpServletRequest,
    ): ApiResponse<Any> {
        val member = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        likeFacade.unlikeProduct(member.memberId, productId)
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/users/me/likes")
    fun findMyLikes(
        request: HttpServletRequest,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<LikeV1Dto.LikeResponse>> {
        val member = request.getAttribute(
            JwtAuthenticationFilter.AUTHENTICATED_MEMBER_ATTRIBUTE,
        ) as AuthenticatedMember

        val likes = likeService.findByUserId(member.memberId, pageable)
        return ApiResponse.success(likes.map { LikeV1Dto.LikeResponse.from(it) })
    }
}

class LikeV1Dto {
    data class LikeResponse(
        val id: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(like: LikeModel): LikeResponse {
                return LikeResponse(
                    id = like.id,
                    productId = like.productId,
                )
            }
        }
    }
}
