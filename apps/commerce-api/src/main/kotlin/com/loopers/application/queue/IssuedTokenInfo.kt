package com.loopers.application.queue

data class IssuedTokenInfo(
    val userId: Long,
    val token: String,
) {
    override fun toString(): String {
        val masked = if (token.length > 4) "${"*".repeat(token.length - 4)}${token.takeLast(4)}" else "****"
        return "IssuedTokenInfo(userId=$userId, token=$masked)"
    }
}
