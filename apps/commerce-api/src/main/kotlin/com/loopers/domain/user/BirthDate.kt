package com.loopers.domain.user

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

data class BirthDate(val value: LocalDate) {
    init {
        require(!value.isAfter(LocalDate.now())) { "생년월일은 미래일 수 없습니다." }
    }

    fun toCompactString(): String = value.format(COMPACT_FORMATTER)

    companion object {
        private val INPUT_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT)
        private val COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun from(dateString: String): BirthDate {
            require(dateString.isNotBlank()) { "생년월일은 빈 문자열일 수 없습니다." }

            val localDate = try {
                LocalDate.parse(dateString, INPUT_FORMATTER)
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException("올바른 날짜 형식이 아닙니다. (yyyy-MM-dd)")
            }

            return BirthDate(localDate)
        }
    }
}
