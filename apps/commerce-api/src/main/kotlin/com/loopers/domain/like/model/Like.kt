package com.loopers.domain.like.model

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Like(
    val id: Long = 0,
    val refUserId: UserId,
    val refProductId: ProductId,
) {
    init {
        if (refUserId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refUserId는 양수여야 합니다.")
        if (refProductId.value <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refProductId는 양수여야 합니다.")
    }
}
