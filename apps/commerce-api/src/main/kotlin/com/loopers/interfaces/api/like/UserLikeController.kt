package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.application.user.AuthenticatedUserInfo
import com.loopers.interfaces.common.ApiResponse
import com.loopers.support.auth.AuthenticatedUser
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
        @AuthenticatedUser userInfo: AuthenticatedUserInfo,
        @PathVariable userId: Long,
    ): ApiResponse<List<LikeDto.UserLikeResponse>> {
        val likes = likeFacade.getUserLikes(userInfo.id, userId)
        return ApiResponse.success(likes.map { LikeDto.UserLikeResponse.from(it) })
    }
}
