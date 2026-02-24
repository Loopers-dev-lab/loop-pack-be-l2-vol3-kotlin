package com.loopers.application.user.model

data class UserChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String,
) {
    override fun toString(): String =
        "UserChangePasswordCommand(currentPassword=[PROTECTED], newPassword=[PROTECTED])"
}
