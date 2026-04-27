package com.loopers.batch.job.ranking

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

fun parseTargetDate(targetDateStr: String?): LocalDate {
    return targetDateStr?.let {
        LocalDate.parse(it, DATE_FORMATTER)
    } ?: LocalDate.now()
}
