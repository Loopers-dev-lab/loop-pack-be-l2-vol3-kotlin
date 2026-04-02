package com.loopers.job.queue

import com.loopers.batch.job.queue.QueueTokenJobConfig
import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueueTokenRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.Job
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${QueueTokenJobConfig.JOB_NAME}", "queue.enabled=true"])
class QueueTokenJobE2ETest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(QueueTokenJobConfig.JOB_NAME) private val job: Job,
    private val queueRepository: QueueRepository,
    private val queueTokenRepository: QueueTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("대기열에 사용자가 있으면, 토큰을 발급하고 대기열에서 제거한다.")
    @Test
    fun issuesTokensAndRemovesFromQueue() {
        // arrange
        jobLauncherTestUtils.job = job
        (1L..5L).forEach { userId ->
            queueRepository.addIfAbsent(userId, System.currentTimeMillis().toDouble() + userId)
        }

        // act
        val jobExecution = jobLauncherTestUtils.launchJob()

        // assert
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(queueRepository.getSize()).isEqualTo(0L) },
            { (1L..5L).forEach { assertThat(queueTokenRepository.hasToken(it)).isTrue() } },
        )
    }

    @DisplayName("대기열이 비어있으면, 정상 완료되고 토큰은 발급되지 않는다.")
    @Test
    fun completesSuccessfully_whenQueueEmpty() {
        // arrange
        jobLauncherTestUtils.job = job

        // act
        val jobExecution = jobLauncherTestUtils.launchJob()

        // assert
        assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode)
    }
}
