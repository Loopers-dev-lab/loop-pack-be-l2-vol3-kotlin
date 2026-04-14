package com.loopers.job.ranking

import com.loopers.batch.interfaces.TriggerResponse
import com.loopers.batch.job.ranking.MonthlyRankingJobConfig
import com.loopers.batch.job.ranking.WeeklyRankingJobConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(MySqlTestContainersConfig::class, BatchRankingControllerFailTest.FailingJobConfig::class)
@ActiveProfiles("scheduler", "test")
@TestPropertySource(properties = ["spring.main.allow-bean-definition-overriding=true"])
class BatchRankingControllerFailTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @TestConfiguration
    class FailingJobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
    ) {
        @Bean(WeeklyRankingJobConfig.JOB_NAME)
        fun failingWeeklyRankingJob(): Job {
            val step: Step =
                StepBuilder("failingWeeklyStep", jobRepository)
                    .tasklet({ _, _ ->
                        throw RuntimeException("weekly 강제 실패")
                    }, transactionManager)
                    .build()
            return JobBuilder(WeeklyRankingJobConfig.JOB_NAME, jobRepository)
                .start(step)
                .build()
        }

        @Bean(MonthlyRankingJobConfig.JOB_NAME)
        fun failingMonthlyRankingJob(): Job {
            val step: Step =
                StepBuilder("failingMonthlyStep", jobRepository)
                    .tasklet({ _, _ ->
                        throw RuntimeException("monthly 강제 실패")
                    }, transactionManager)
                    .build()
            return JobBuilder(MonthlyRankingJobConfig.JOB_NAME, jobRepository)
                .start(step)
                .build()
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /internal/batch/ranking/weekly — Job 실패 → 500 FAILED")
    @Test
    fun weeklyTrigger_jobFails_returns500Failed() {
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/weekly",
                mapOf("baseDate" to "20260414"),
                TriggerResponse::class.java,
            )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR) },
            { assertThat(response.body?.status).isEqualTo("FAILED") },
            { assertThat(response.body?.message).isNotBlank() },
        )
    }

    @DisplayName("POST /internal/batch/ranking/monthly — Job 실패 → 500 FAILED")
    @Test
    fun monthlyTrigger_jobFails_returns500Failed() {
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/monthly",
                mapOf("baseDate" to "20260414"),
                TriggerResponse::class.java,
            )

        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR) },
            { assertThat(response.body?.status).isEqualTo("FAILED") },
            { assertThat(response.body?.message).isNotBlank() },
        )
    }
}
