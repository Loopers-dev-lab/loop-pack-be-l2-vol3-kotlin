package com.loopers.application.like

import com.loopers.application.SliceResult
import com.loopers.application.catalog.UserGetProductResult

data class LikedProductsResult(
    val content: List<UserGetProductResult>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(sliceResult: SliceResult<UserGetProductResult>): LikedProductsResult {
            return LikedProductsResult(
                content = sliceResult.content,
                page = sliceResult.page,
                size = sliceResult.size,
                hasNext = sliceResult.hasNext,
            )
        }
    }
}
