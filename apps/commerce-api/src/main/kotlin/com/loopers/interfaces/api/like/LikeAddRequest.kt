package com.loopers.interfaces.api.like

import jakarta.validation.constraints.NotNull

data class LikeAddRequest(
    @field:NotNull(message = "상품 ID는 필수입니다.")
    val productId: Long,
)
