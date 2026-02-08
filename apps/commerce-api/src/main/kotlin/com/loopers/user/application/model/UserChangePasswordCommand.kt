package com.loopers.user.application.model

data class UserChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String,
)
