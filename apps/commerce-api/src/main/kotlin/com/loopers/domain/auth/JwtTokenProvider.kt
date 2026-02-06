package com.loopers.domain.auth

interface JwtTokenProvider {
    fun generateToken(memberId: Long, loginId: String): String
    fun validateToken(token: String): AuthenticatedMember
}
