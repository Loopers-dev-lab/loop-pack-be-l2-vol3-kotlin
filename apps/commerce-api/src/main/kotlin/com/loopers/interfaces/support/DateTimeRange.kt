package com.loopers.interfaces.support

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

class DateTimeRange(from: String, to: String) {

    val from: ZonedDateTime = try {
        ZonedDateTime.parse(from)
    } catch (e: DateTimeParseException) {
        throw CoreException(ErrorType.BAD_REQUEST, "날짜/시간 형식이 올바르지 않습니다: '$from'")
    }
    val to: ZonedDateTime = try {
        ZonedDateTime.parse(to)
    } catch (e: DateTimeParseException) {
        throw CoreException(ErrorType.BAD_REQUEST, "날짜/시간 형식이 올바르지 않습니다: '$to'")
    }

    init {
        if (this.from.isAfter(this.to)) {
            throw CoreException(ErrorType.BAD_REQUEST, "조회 시작일(from)은 종료일(to)보다 이후일 수 없습니다.")
        }
    }

    companion object {
        fun of(from: String?, to: String?): DateTimeRange {
            val now = ZonedDateTime.now()
            val defaultFrom = now.minusMonths(1)
            return DateTimeRange(
                from ?: defaultFrom.toString(),
                to ?: now.toString(),
            )
        }
    }
}
