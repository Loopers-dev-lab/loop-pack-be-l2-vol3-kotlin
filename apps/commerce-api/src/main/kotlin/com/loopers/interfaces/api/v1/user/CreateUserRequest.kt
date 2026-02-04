package com.loopers.interfaces.api.v1.user

import com.loopers.application.user.RegisterUserCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserRequest(

    @field:NotBlank(message = "로그인 ID는 필수입니다.")
    @field:Size(max = 10, message = "로그인 ID는 10자 이내여야 합니다.")
    val loginId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 8~16자여야 합니다.")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,

    @field:NotBlank(message = "생년월일은 필수입니다.")
    val birthDate: String,

    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    val email: String,

    @field:NotBlank(message = "성별은 필수입니다.")
    val gender: String,
) {
    fun toCommand() = RegisterUserCommand(
        loginId = loginId,
        password = password,
        name = name,
        birthDate = birthDate,
        email = email,
        gender = gender,
    )
}
