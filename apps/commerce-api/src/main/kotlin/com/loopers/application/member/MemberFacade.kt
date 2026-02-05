package com.loopers.application.member

import com.loopers.domain.member.MemberService
import com.loopers.domain.member.vo.LoginId
import org.springframework.stereotype.Component

@Component
class MemberFacade(
    private val memberService: MemberService,
) {

    /**
     * 내 정보를 조회합니다.
     * 이름은 마지막 글자가 마스킹 처리됩니다.
     */
    fun getMyProfile(loginId: String): MemberInfo.MyProfile {
        val member = memberService.getMemberByLoginId(LoginId(loginId))
        return MemberInfo.MyProfile.from(member)
    }

    /**
     * 비밀번호를 변경합니다.
     */
    fun changePassword(command: ChangePasswordCommand) {
        memberService.changePassword(
            loginId = LoginId(command.loginId),
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
