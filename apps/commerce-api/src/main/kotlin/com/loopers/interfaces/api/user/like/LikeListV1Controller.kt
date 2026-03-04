package com.loopers.interfaces.api.user.like

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.like.UserProductLikeListUseCase
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeListV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val listUseCase: UserProductLikeListUseCase,
) : LikeListV1ApiSpec {
    @GetMapping("/api/v1/users/me/likes")
    override fun getMyLikes(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @ModelAttribute pageRequest: PageRequest,
    ): ApiResponse<PageResponse<LikeV1Response.LikedProduct>> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        val result = listUseCase.getList(userId, pageRequest)
        val response = PageResponse(
            content = result.content.map { LikeV1Response.LikedProduct.from(it) },
            totalElements = result.totalElements,
            page = result.page,
            size = result.size,
        )
        return ApiResponse.success(response)
    }
}
