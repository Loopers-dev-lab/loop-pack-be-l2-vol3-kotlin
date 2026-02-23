package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.domain.user.User
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserLikeController(
    private val likeFacade: LikeFacade,
) : UserLikeApiSpec {

    @GetMapping("/{userId}/likes")
    override fun getUserLikes(
        @AuthenticatedUser user: User,
        @PathVariable userId: Long,
    ): ApiResponse<List<LikeDto.UserLikeResponse>> {
        if (user.id != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "타 유저의 정보에 접근할 수 없습니다")
        }
        val likes = likeFacade.getUserLikes(userId)
        return ApiResponse.success(likes.map { LikeDto.UserLikeResponse.from(it) })
    }
}
