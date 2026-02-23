package com.loopers.domain.user

interface UserPasswordHasher {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
