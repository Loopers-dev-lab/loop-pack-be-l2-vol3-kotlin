package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@JvmInline
value class BirthDate(val value: String) {

    fun validate() {
        validateNotBlank()
        validateFormat()
        validateValidDate()
    }

    private fun validateNotBlank() {
        if (value.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.")
        }
    }

    private fun validateFormat() {
        if (value.length != 8) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyyMMdd 형식이어야 합니다.")
        }

        if (!value.all { it.isDigit() }) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyyMMdd 형식이어야 합니다.")
        }
    }

    private fun validateValidDate() {
        try {
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
            val parsedDate = LocalDate.parse(value, formatter)

            val formattedBack = formatter.format(parsedDate)
            if (formattedBack != value) {
                throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 유효한 날짜여야 합니다.")
            }
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 유효한 날짜여야 합니다.")
        }
    }

    companion object {
        private const val DATE_FORMAT = "yyyyMMdd"

        fun of(birthDate: String): BirthDate {
            val bd = BirthDate(birthDate)
            bd.validate()
            return bd
        }
    }
}
