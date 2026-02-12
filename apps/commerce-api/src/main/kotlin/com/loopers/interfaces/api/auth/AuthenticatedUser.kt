package com.loopers.interfaces.api.auth

import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.LoginId

data class AuthenticatedUser(
    val loginId: LoginId,
    val birthDate: BirthDate,
)
