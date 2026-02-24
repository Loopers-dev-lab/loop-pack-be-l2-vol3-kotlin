package com.loopers.domain.like.model

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Like(
    refUserId: Long,
    refProductId: Long,
) {

    init {
        if (refUserId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refUserId는 양수여야 합니다.")
        if (refProductId <= 0) throw CoreException(ErrorType.BAD_REQUEST, "refProductId는 양수여야 합니다.")
    }

    val id: Long = 0

    var refUserId: Long = refUserId
        private set

    var refProductId: Long = refProductId
        private set
}
