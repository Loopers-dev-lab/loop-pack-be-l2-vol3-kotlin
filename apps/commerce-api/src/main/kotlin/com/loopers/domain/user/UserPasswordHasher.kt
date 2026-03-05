package com.loopers.domain.user

interface UserPasswordHasher {
    fun encode(rawPassword: RawPassword): EncodedPassword
    fun matches(rawPassword: RawPassword, encodedPassword: EncodedPassword): Boolean
}
