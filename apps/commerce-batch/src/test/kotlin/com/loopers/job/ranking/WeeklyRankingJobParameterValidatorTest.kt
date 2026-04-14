package com.loopers.job.ranking

import com.loopers.batch.job.ranking.WeeklyRankingJobParameterValidator
import com.loopers.batch.job.ranking.WeeklyWindow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersInvalidException
import java.time.LocalDate
import java.time.format.DateTimeParseException

class WeeklyRankingJobParameterValidatorTest {

    private val validator = WeeklyRankingJobParameterValidator()

    @Nested
    @DisplayName("validate")
    inner class Validate {

        @DisplayName("baseDate 파라미터가 누락되면 JobParametersInvalidException이 발생한다")
        @Test
        fun throwsExceptionWhenBaseDateIsMissing() {
            val parameters = JobParametersBuilder().toJobParameters()

            assertThatThrownBy { validator.validate(parameters) }
                .isInstanceOf(JobParametersInvalidException::class.java)
        }

        @DisplayName("baseDate 형식이 yyyyMMdd가 아니면 JobParametersInvalidException이 발생한다")
        @Test
        fun throwsExceptionWhenBaseDateFormatIsInvalid() {
            val parameters = JobParametersBuilder()
                .addString("baseDate", "2024-01-15")
                .toJobParameters()

            assertThatThrownBy { validator.validate(parameters) }
                .isInstanceOf(JobParametersInvalidException::class.java)
        }

        @DisplayName("존재하지 않는 날짜(20240230)가 주어지면 JobParametersInvalidException이 발생한다")
        @Test
        fun throwsExceptionForNonExistentDate() {
            val parameters = JobParametersBuilder()
                .addString("baseDate", "20240230")
                .toJobParameters()

            assertThatThrownBy { validator.validate(parameters) }
                .isInstanceOf(JobParametersInvalidException::class.java)
        }

        @DisplayName("존재하지 않는 월(20241301)이 주어지면 JobParametersInvalidException이 발생한다")
        @Test
        fun throwsExceptionForNonExistentMonth() {
            val parameters = JobParametersBuilder()
                .addString("baseDate", "20241301")
                .toJobParameters()

            assertThatThrownBy { validator.validate(parameters) }
                .isInstanceOf(JobParametersInvalidException::class.java)
        }

        @DisplayName("올바른 baseDate(yyyyMMdd)가 주어지면 예외가 발생하지 않는다")
        @Test
        fun doesNotThrowForValidBaseDate() {
            val parameters = JobParametersBuilder()
                .addString("baseDate", "20240115")
                .toJobParameters()

            validator.validate(parameters)
        }
    }

    @Nested
    @DisplayName("WeeklyWindow.from")
    inner class WeeklyWindowFrom {

        @ParameterizedTest
        @ValueSource(strings = ["20240230", "20241301"])
        @DisplayName("존재하지 않는 날짜가 주어지면 DateTimeParseException이 발생한다")
        fun throwsExceptionForInvalidDate(baseDate: String) {
            assertThatThrownBy { WeeklyWindow.from(baseDate) }
                .isInstanceOf(DateTimeParseException::class.java)
        }

        @DisplayName("2024-01-15(월요일)의 periodKey, startDate, endDate가 올바르게 계산된다")
        @Test
        fun calculatesCorrectWindowForMonday() {
            val (periodKey, startDate, endDate) = WeeklyWindow.from("20240115")

            assertThat(periodKey).isEqualTo("2024-W03")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 15))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 1, 21))
        }

        @DisplayName("연도 경계(2024-12-31)에서 ISO 8601 기준 periodKey가 다음 연도로 넘어간다")
        @Test
        fun calculatesCorrectWindowAtYearBoundary() {
            // 2024-12-31(화요일)이 속한 주: Mon 2024-12-30 ~ Sun 2025-01-05 = ISO 2025-W01
            val (periodKey, startDate, endDate) = WeeklyWindow.from("20241231")

            assertThat(periodKey).isEqualTo("2025-W01")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 12, 30))
            assertThat(endDate).isEqualTo(LocalDate.of(2025, 1, 5))
        }
    }
}
