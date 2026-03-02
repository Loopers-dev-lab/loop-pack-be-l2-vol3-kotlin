package com.loopers.interfaces.api.like

import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.CancelLikeUseCase
import com.loopers.application.like.GetMyLikesUseCase
import com.loopers.application.like.LikeCommand
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.auth.CurrentUserId
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ApiPaths.Likes.BASE)
class LikeV1Controller(
    private val addLikeUseCase: AddLikeUseCase,
    private val cancelLikeUseCase: CancelLikeUseCase,
    private val getMyLikesUseCase: GetMyLikesUseCase,
) : LikeV1ApiSpec {

    @PostMapping
    override fun addLike(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: LikeAddRequest,
    ): ApiResponse<Unit> {
        addLikeUseCase.execute(
            LikeCommand.Create(
                userId = userId,
                productId = request.productId,
            ),
        )
        return ApiResponse.success(Unit)
    }

    @DeleteMapping("/{productId}")
    override fun cancelLike(
        @CurrentUserId userId: Long,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        cancelLikeUseCase.execute(userId, productId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/me")
    override fun getMyLikes(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PageResult<LikeProductResponse>> {
        val result = getMyLikesUseCase.execute(userId, page, size)
        val response = PageResult.of(
            content = result.content.map { LikeProductResponse.from(it) },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
        )
        return ApiResponse.success(response)
    }
}
