package com.loopers.application.user

data class ChangePasswordCommand(
    val oldPassword: String,
    val newPassword: String,
) {
    override fun toString(): String = "ChangePasswordCommand(oldPassword=****, newPassword=****)"
}
