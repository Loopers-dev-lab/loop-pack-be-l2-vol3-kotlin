package com.loopers.application.user.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("User Command toString 마스킹")
class UserCommandTest {
    @Nested
    @DisplayName("UserSignUpCommand toString에 비밀번호가 포함되지 않는다")
    inner class SignUpCommandToString {
        @Test
        @DisplayName("toString 호출 시 password가 [PROTECTED]로 마스킹된다")
        fun toString_passwordMasked() {
            val command = UserSignUpCommand(
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            )
            val result = command.toString()
            assertThat(result).doesNotContain("Password1!")
            assertThat(result).contains("[PROTECTED]")
        }
    }

    @Nested
    @DisplayName("UserChangePasswordCommand toString에 비밀번호가 포함되지 않는다")
    inner class ChangePasswordCommandToString {
        @Test
        @DisplayName("toString 호출 시 currentPassword, newPassword가 [PROTECTED]로 마스킹된다")
        fun toString_passwordsMasked() {
            val command = UserChangePasswordCommand(
                currentPassword = "OldPassword1!",
                newPassword = "NewPassword1!",
            )
            val result = command.toString()
            assertThat(result).doesNotContain("OldPassword1!")
            assertThat(result).doesNotContain("NewPassword1!")
            assertThat(result).contains("[PROTECTED]")
        }
    }
}
