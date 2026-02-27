package com.loopers.support.auth

interface Authenticator {
    fun authenticate(loginId: String, password: String): AuthenticatedUserInfo
}
