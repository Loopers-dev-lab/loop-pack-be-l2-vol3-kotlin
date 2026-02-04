package com.loopers.application.user

data class ChangePasswordCommand(
    val oldPassword: String,
    val newPassword: String,
)
