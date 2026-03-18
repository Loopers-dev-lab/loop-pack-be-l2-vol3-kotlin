package com.loopers.infrastructure.pg

data class PgApiResponse<T>(
    val meta: PgMeta,
    val data: T?,
) {
    data class PgMeta(
        val result: String,
        val errorCode: String?,
        val message: String?,
    )

    fun isSuccess(): Boolean = meta.result == "SUCCESS"
}
