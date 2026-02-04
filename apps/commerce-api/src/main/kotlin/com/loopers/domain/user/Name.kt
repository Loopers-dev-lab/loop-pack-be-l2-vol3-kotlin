package com.loopers.domain.user

data class Name(val value: String) {
    init {
        require(value.isNotBlank()) { "이름은 빈 문자열일 수 없습니다." }
    }

    /**
     * Mask the name by replacing its last character with an asterisk, or return a single asterisk for one-character names.
     *
     * @return "`*`" for single-character names; otherwise the name with its last character replaced by "`*`".
     */
    fun masked(): String {
        return if (value.length == 1) {
            "*"
        } else {
            value.dropLast(1) + "*"
        }
    }
}