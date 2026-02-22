package com.loopers.support.error

class BrandException private constructor(
    errorCode: BrandErrorCode,
    message: String = errorCode.message,
    cause: Throwable? = null,
) : CoreException(errorCode, message, cause) {

    companion object {
        fun notFound() = BrandException(BrandErrorCode.BRAND_NOT_FOUND)

        fun duplicateName() = BrandException(BrandErrorCode.DUPLICATE_BRAND_NAME)

        fun invalidName() = BrandException(BrandErrorCode.INVALID_BRAND_NAME)
    }
}
