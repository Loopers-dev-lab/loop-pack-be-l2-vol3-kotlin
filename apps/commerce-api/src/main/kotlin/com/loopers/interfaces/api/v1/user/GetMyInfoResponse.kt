package com.loopers.interfaces.api.v1.user

import com.loopers.application.user.UserInfo

data class GetMyInfoResponse(
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
    val gender: String,
) {
    companion object {
        fun from(userInfo: UserInfo) = GetMyInfoResponse(
            loginId = userInfo.loginId,
            name = userInfo.name,
            birthDate = userInfo.birthDate,
            email = userInfo.email,
            gender = userInfo.gender,
        )
    }
}
