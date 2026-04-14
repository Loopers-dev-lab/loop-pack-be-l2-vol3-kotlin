package com.loopers

import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
class CommerceBatchApplicationTest @Autowired constructor(
    private val jobLauncher: JobLauncher,
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jdbcTemplate: JdbcTemplate,
) {
    @Test
    fun contextLoads() {
    }

    @DisplayName("JobLauncher, JobRepository, TransactionManager가 정상 주입된다")
    @Test
    fun batchInfraBeansAreInjected() {
        assertThat(jobLauncher).isNotNull()
        assertThat(jobRepository).isNotNull()
        assertThat(transactionManager).isNotNull()
    }

    @DisplayName("Spring Batch 메타 테이블 6개가 DB에 모두 존재한다")
    @Test
    fun batchMetaTablesExist() {
        val requiredTables = listOf(
            "BATCH_JOB_INSTANCE",
            "BATCH_JOB_EXECUTION",
            "BATCH_JOB_EXECUTION_PARAMS",
            "BATCH_STEP_EXECUTION",
            "BATCH_STEP_EXECUTION_CONTEXT",
            "BATCH_JOB_EXECUTION_CONTEXT",
        )
        val actualTables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN (${requiredTables.joinToString(",") { "?" }})",
            String::class.java,
            *requiredTables.toTypedArray(),
        )
        assertThat(actualTables).containsExactlyInAnyOrderElementsOf(requiredTables)
    }
}
