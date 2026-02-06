package com.loopers.domain.auth

data class AuthenticatedMember(
    val memberId: Long,
    val loginId: String,
)
