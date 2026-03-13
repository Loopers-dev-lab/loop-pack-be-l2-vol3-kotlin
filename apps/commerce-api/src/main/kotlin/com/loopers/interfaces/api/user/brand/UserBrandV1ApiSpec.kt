package com.loopers.interfaces.api.user.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "[User] Brand V1 API", description = "[User] Brand API 입니다.")
interface UserBrandV1ApiSpec {
    @Operation(summary = "브랜드 상세 조회", description = "활성 상태의 브랜드 상세 정보를 조회합니다.")
    fun getDetail(brandId: Long): ApiResponse<UserBrandV1Response.Detail>
}
