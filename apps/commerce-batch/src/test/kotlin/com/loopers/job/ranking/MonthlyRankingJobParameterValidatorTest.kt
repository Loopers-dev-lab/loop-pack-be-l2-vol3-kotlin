package com.loopers.job.ranking

import com.loopers.batch.job.ranking.MonthlyRankingJobParameterValidator
import com.loopers.batch.job.ranking.MonthlyWindow
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.JobParametersInvalidException
import java.time.LocalDate

class MonthlyRankingJobParameterValidatorTest {

    private val validator = MonthlyRankingJobParameterValidator()

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
    @DisplayName("MonthlyWindow.from")
    inner class MonthlyWindowFrom {

        @DisplayName("월 중간일(20240115)이 주어지면 해당 월 전체를 커버하는 window가 반환된다")
        @Test
        fun calculatesCorrectWindowForMidMonth() {
            val (periodKey, startDate, endDate) = MonthlyWindow.from("20240115")

            assertThat(periodKey).isEqualTo("2024-01")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 1, 31))
        }

        @DisplayName("윤년 2월(20240215)의 endDate는 2024-02-29이다")
        @Test
        fun calculatesCorrectWindowForLeapYearFebruary() {
            val (periodKey, startDate, endDate) = MonthlyWindow.from("20240215")

            assertThat(periodKey).isEqualTo("2024-02")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 2, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 2, 29))
        }

        @DisplayName("평년 2월(20230215)의 endDate는 2023-02-28이다")
        @Test
        fun calculatesCorrectWindowForNonLeapYearFebruary() {
            val (periodKey, startDate, endDate) = MonthlyWindow.from("20230215")

            assertThat(periodKey).isEqualTo("2023-02")
            assertThat(startDate).isEqualTo(LocalDate.of(2023, 2, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2023, 2, 28))
        }

        @DisplayName("31일 월(20240331)의 endDate는 2024-03-31이다")
        @Test
        fun calculatesCorrectWindowFor31DayMonth() {
            val (periodKey, startDate, endDate) = MonthlyWindow.from("20240331")

            assertThat(periodKey).isEqualTo("2024-03")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 3, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 3, 31))
        }

        @DisplayName("30일 월(20240430)의 endDate는 2024-04-30이다")
        @Test
        fun calculatesCorrectWindowFor30DayMonth() {
            val (periodKey, startDate, endDate) = MonthlyWindow.from("20240430")

            assertThat(periodKey).isEqualTo("2024-04")
            assertThat(startDate).isEqualTo(LocalDate.of(2024, 4, 1))
            assertThat(endDate).isEqualTo(LocalDate.of(2024, 4, 30))
        }
    }
}
