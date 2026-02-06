package com.loopers.interfaces.api.member

import com.loopers.application.member.MemberFacade
import com.loopers.application.member.MemberInfo
import java.time.LocalDate

class MemberV1Dto {

    data class MyProfileResponse(
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: MemberInfo.MyProfile) = MyProfileResponse(
                loginId = info.loginId,
                name = info.name,
                birthDate = info.birthDate,
                email = info.email,
            )
        }
    }

    data class ChangePasswordRequest(
        val currentPassword: String,
        val newPassword: String,
    ) {
        fun toCommand(loginId: String) = MemberFacade.ChangePasswordCommand(
            loginId = loginId,
            currentPassword = currentPassword,
            newPassword = newPassword,
        )

        override fun toString(): String = "ChangePasswordRequest(currentPassword=****, newPassword=****)"
    }
}
