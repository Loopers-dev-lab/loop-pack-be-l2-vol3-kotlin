package com.loopers.application.auth

import java.security.MessageDigest

data class CachedAuth(
    val memberId: Long,
    val loginId: String,
    val passwordDigest: String,
) {
    fun matchesPassword(rawPassword: String): Boolean {
        return passwordDigest == sha256(rawPassword)
    }

    fun toAuthResult(): AuthResult {
        return AuthResult(id = memberId, loginId = loginId)
    }

    companion object {
        fun of(authResult: AuthResult, rawPassword: String): CachedAuth {
            return CachedAuth(
                memberId = authResult.id,
                loginId = authResult.loginId,
                passwordDigest = sha256(rawPassword),
            )
        }

        private fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(input.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}
