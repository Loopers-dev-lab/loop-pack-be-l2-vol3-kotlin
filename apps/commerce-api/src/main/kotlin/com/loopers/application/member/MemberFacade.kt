package com.loopers.application.member

import com.loopers.domain.member.MemberPasswordChanger
import com.loopers.domain.member.MemberReader
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberFacade(
    private val memberReader: MemberReader,
    private val memberPasswordChanger: MemberPasswordChanger,
) {

    /**
     * 내 정보를 조회합니다.
     * 이름은 마지막 글자가 마스킹 처리됩니다.
     */
    @Transactional(readOnly = true)
    fun getMyProfile(loginId: String): MemberInfo.MyProfile {
        val member = memberReader.getByLoginId(loginId)
        return MemberInfo.MyProfile.from(member)
    }

    /**
     * 비밀번호를 변경합니다.
     */
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
