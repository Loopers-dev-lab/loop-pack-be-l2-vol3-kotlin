package com.loopers.interfaces.api.like

import com.loopers.application.auth.AuthUseCase
import com.loopers.application.like.LikeUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/likes")
class LikeV1Controller(
    private val authUseCase: AuthUseCase,
    private val likeUseCase: LikeUseCase,
) : LikeV1ApiSpec {

    @PostMapping
    override fun register(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @RequestBody request: LikeV1Dto.RegisterRequest,
    ): ApiResponse<LikeV1Dto.RegisteredResponse> {
        val member = authUseCase.authenticate(loginId, password)

        return likeUseCase.register(member.id!!, request.productId)
            .let { LikeV1Dto.RegisteredResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{id}")
    override fun remove(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable id: Long,
    ): ApiResponse<Any> {
        val member = authUseCase.authenticate(loginId, password)

        likeUseCase.remove(id, member.id!!)
        return ApiResponse.success()
    }

    @GetMapping("/me")
    override fun getMyLikes(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
    ): ApiResponse<List<LikeV1Dto.DetailResponse>> {
        val member = authUseCase.authenticate(loginId, password)

        return likeUseCase.getMyLikes(member.id!!)
            .map { LikeV1Dto.DetailResponse.from(it) }
            .let { ApiResponse.success(it) }
    }
}
