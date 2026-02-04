package com.loopers.interfaces.api.v1.user

import com.loopers.application.user.UserInfo

data class GetMyInfoResponse(
    val loginId: String,
    val name: String,
    val birthDate: String,
    val email: String,
    val gender: String,
) {
    companion object {
        /**
         * Create a GetMyInfoResponse from the given UserInfo.
         *
         * The returned response copies loginId, birthDate, email, and gender from `userInfo`.
         * The `name` is masked: single-character names become `"*"`, otherwise the last character is replaced with `"*"`.
         *
         * @param userInfo Source user information to map from.
         * @return A GetMyInfoResponse with the masked name and other fields copied from `userInfo`.
         */
        fun from(userInfo: UserInfo) = GetMyInfoResponse(
            loginId = userInfo.loginId,
            name = maskName(userInfo.name),
            birthDate = userInfo.birthDate,
            email = userInfo.email,
            gender = userInfo.gender,
        )

        /**
         * Masks a person's name by replacing its last character with an asterisk.
         *
         * @param name The original name to mask.
         * @return `*` for empty or single-character names; otherwise the name with its last character replaced by `*`.
         */
        private fun maskName(name: String): String {
            return if (name.length <= 1) "*" else name.dropLast(1) + "*"
        }
    }
}