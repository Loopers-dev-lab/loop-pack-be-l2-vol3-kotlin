package com.loopers.batch.job.ranking

import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersInvalidException
import org.springframework.batch.core.JobParametersValidator
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class MonthlyWindow(
    val periodKey: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun from(baseDate: String): MonthlyWindow {
            val date = LocalDate.parse(baseDate, FORMATTER)
            val yearMonth = YearMonth.from(date)
            val periodKey = yearMonth.toString()
            val startDate = yearMonth.atDay(1)
            val endDate = yearMonth.atEndOfMonth()
            return MonthlyWindow(periodKey, startDate, endDate)
        }
    }
}

class MonthlyRankingJobParameterValidator : JobParametersValidator {
    companion object {
        private val BASE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
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
