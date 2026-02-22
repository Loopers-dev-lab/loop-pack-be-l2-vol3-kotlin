package com.loopers.domain.common.vo

@JvmInline
value class Email(val value: String) {
    companion object {
        fun of(value: String): Email {
            return Email(value)
        }
    }
}
