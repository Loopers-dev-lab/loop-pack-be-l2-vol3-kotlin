package com.loopers.application.member

import com.loopers.domain.member.MemberPasswordChanger
import com.loopers.domain.member.MemberReader
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberUseCase(
    private val memberReader: MemberReader,
    private val memberPasswordChanger: MemberPasswordChanger,
) {

    @Transactional(readOnly = true)
    fun getMyProfile(loginId: String): MemberInfo.MyProfile {
        val member = memberReader.getByLoginId(loginId)
        return MemberInfo.MyProfile.from(member)
    }

    @Transactional
    fun changePassword(command: ChangePasswordCommand) {
        memberPasswordChanger.changePassword(
            loginId = command.loginId,
            currentRawPassword = command.currentPassword,
            newRawPassword = command.newPassword,
        )
    }

    data class ChangePasswordCommand(
        val loginId: String,
        val currentPassword: String,
        val newPassword: String,
    )
}
