package com.loopers.application.member

import com.loopers.domain.member.MemberModel
import java.time.LocalDate

data class MemberInfo(
    val loginId: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
) {
    companion object {
        fun from(model: MemberModel): MemberInfo {
            return MemberInfo(
                loginId = model.loginId,
                name = model.getMaskedName(),
                birthday = model.birthday,
                email = model.email,
            )
        }
    }
}
