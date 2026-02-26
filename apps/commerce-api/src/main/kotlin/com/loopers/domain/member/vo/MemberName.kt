package com.loopers.domain.member.vo

@JvmInline
value class MemberName(val value: String) {
    fun masked(): String {
        if (value.length <= 1) return "*"
        return value.substring(0, value.length - 1) + "*"
    }

    companion object {
        fun of(value: String): MemberName {
            return MemberName(value)
        }
    }
}
