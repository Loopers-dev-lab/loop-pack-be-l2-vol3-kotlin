package com.loopers.job.ranking

import com.loopers.batch.interfaces.TriggerResponse
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = RANDOM_PORT)
@SpringBatchTest
@Import(MySqlTestContainersConfig::class)
@ActiveProfiles("scheduler", "test")
class BatchRankingControllerTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /internal/batch/ranking/weekly — 유효한 baseDate → 200 COMPLETED")
    @Test
    fun weeklyTrigger_validBaseDate_returns200Completed() {
        // act
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/weekly",
                mapOf("baseDate" to "20260414"),
                TriggerResponse::class.java,
            )

        // assert
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body?.status).isEqualTo("COMPLETED") },
            { assertThat(response.body?.message).isNull() },
        )
    }

    @DisplayName("POST /internal/batch/ranking/monthly — 유효한 baseDate → 200 COMPLETED")
    @Test
    fun monthlyTrigger_validBaseDate_returns200Completed() {
        // act
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/monthly",
                mapOf("baseDate" to "20260414"),
                TriggerResponse::class.java,
            )

        // assert
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body?.status).isEqualTo("COMPLETED") },
            { assertThat(response.body?.message).isNull() },
        )
    }

    @DisplayName("POST /internal/batch/ranking/weekly — 잘못된 baseDate → 400 INVALID")
    @Test
    fun weeklyTrigger_invalidBaseDate_returns400Invalid() {
        // act
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/weekly",
                mapOf("baseDate" to "invalid"),
                TriggerResponse::class.java,
            )

        // assert
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            { assertThat(response.body?.status).isEqualTo("INVALID") },
            { assertThat(response.body?.message).isNotNull() },
        )
    }

    @DisplayName("POST /internal/batch/ranking/monthly — 잘못된 baseDate → 400 INVALID")
    @Test
    fun monthlyTrigger_invalidBaseDate_returns400Invalid() {
        // act
        val response =
            restTemplate.postForEntity(
                "/internal/batch/ranking/monthly",
                mapOf("baseDate" to "nope"),
                TriggerResponse::class.java,
            )

        // assert
        assertAll(
            { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            { assertThat(response.body?.status).isEqualTo("INVALID") },
            { assertThat(response.body?.message).isNotNull() },
        )
    }
}
