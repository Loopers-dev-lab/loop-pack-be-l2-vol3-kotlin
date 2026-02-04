package com.loopers.interfaces.api.v1.user

import com.loopers.application.user.ChangePasswordCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(

    @field:NotBlank(message = "기존 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
    val oldPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
    val newPassword: String,
) {
    fun toCommand() = ChangePasswordCommand(
        oldPassword = oldPassword,
        newPassword = newPassword,
    )
}
