package com.loopers.interfaces.api.user.like

import com.loopers.application.user.auth.UserAuthenticateUseCase
import com.loopers.application.user.like.UserProductLikeCancelUseCase
import com.loopers.application.user.like.UserProductLikeCommand
import com.loopers.application.user.like.UserProductLikeRegisterUseCase
import com.loopers.interfaces.api.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class ProductLikeV1Controller(
    private val userAuthenticateUseCase: UserAuthenticateUseCase,
    private val registerUseCase: UserProductLikeRegisterUseCase,
    private val cancelUseCase: UserProductLikeCancelUseCase,
) : ProductLikeV1ApiSpec {
    @PostMapping("/api/v1/products/{productId}/likes")
    override fun register(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Nothing?> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        registerUseCase.register(UserProductLikeCommand.Register(userId = userId, productId = productId))
        return ApiResponse.success(null)
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun cancel(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") password: String,
        @PathVariable productId: Long,
    ): ApiResponse<Nothing?> {
        val userId = userAuthenticateUseCase.authenticateAndGetId(loginId, password)
        cancelUseCase.cancel(UserProductLikeCommand.Cancel(userId = userId, productId = productId))
        return ApiResponse.success(null)
    }
}
