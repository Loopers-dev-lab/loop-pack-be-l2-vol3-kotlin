package com.loopers.domain.like.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Like(
    val refUserId: Long,
    val refProductId: Long,
) {

    val id: Long = 0

    init {
        if (refUserId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refUserId는 양수여야 합니다.")
        if (refProductId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refProductId는 양수여야 합니다.")
    }
}
