package com.loopers.batch.job.ranking

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.JobParametersValidator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.WeekFields

data class WeeklyWindow(
    val periodKey: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)
        private val WEEK_FIELDS = WeekFields.ISO

        fun from(baseDate: String): WeeklyWindow {
            val date = LocalDate.parse(baseDate, FORMATTER)
            val week = date.get(WEEK_FIELDS.weekOfWeekBasedYear())
            val year = date.get(WEEK_FIELDS.weekBasedYear())
            val periodKey = "%d-W%02d".format(year, week)
            val startDate = date.with(WEEK_FIELDS.dayOfWeek(), 1)
            val endDate = date.with(WEEK_FIELDS.dayOfWeek(), 7)
            return WeeklyWindow(periodKey, startDate, endDate)
        }
    }
}

class WeeklyRankingJobParameterValidator : JobParametersValidator {
    companion object {
        private val BASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT)
    }

    override fun validate(parameters: JobParameters?) {
        val baseDate = parameters?.getString("baseDate")
            ?: throw JobParametersInvalidException("baseDate is required")

        try {
            LocalDate.parse(baseDate, BASE_DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            throw JobParametersInvalidException("baseDate must be in yyyyMMdd format, but was: $baseDate")
        }
    }
}
