package com.loopers.job.likecount

import com.loopers.batch.job.likecount.LikeCountSyncJobConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@SpringBatchTest
@TestPropertySource(properties = ["spring.batch.job.name=${LikeCountSyncJobConfig.JOB_NAME}"])
class LikeCountSyncJobTest @Autowired constructor(
    private val jobLauncherTestUtils: JobLauncherTestUtils,
    @param:Qualifier(LikeCountSyncJobConfig.JOB_NAME) private val job: Job,
    private val jdbcTemplate: JdbcTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @BeforeEach
    fun setUp() {
        // 브랜드 생성
        jdbcTemplate.update(
            "INSERT INTO brand (name, description, image_url, status, created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())",
            "TestBrand",
            "테스트 브랜드",
            "https://example.com/brand.jpg",
        )
        val brandId = jdbcTemplate.queryForObject("SELECT id FROM brand LIMIT 1", Long::class.java)!!

        // 상품 3개 생성
        for (i in 1..3) {
            jdbcTemplate.update(
                """
                INSERT INTO product (brand_id, name, description, price, stock_quantity, like_count, image_url, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, 'ACTIVE', NOW(), NOW())
                """.trimIndent(),
                brandId,
                "Product_$i",
                "설명_$i",
                10000L * i,
                100,
                "https://example.com/product_$i.jpg",
            )
        }
        val productIds = jdbcTemplate.queryForList("SELECT id FROM product ORDER BY id", Long::class.java)

        // 상품1: 좋아요 5개, 상품2: 좋아요 3개, 상품3: 좋아요 0개
        for (memberId in 1L..5L) {
            jdbcTemplate.update(
                "INSERT INTO product_like (product_id, member_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                productIds[0],
                memberId,
            )
        }
        for (memberId in 1L..3L) {
            jdbcTemplate.update(
                "INSERT INTO product_like (product_id, member_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                productIds[1],
                memberId,
            )
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("likeCountSyncJob 실행 시 product.like_count가 product_like 기준으로 갱신된다.")
    @Test
    fun shouldSyncLikeCount() {
        // arrange
        jobLauncherTestUtils.job = job

        // act
        val jobExecution = jobLauncherTestUtils.launchJob()

        // assert
        val likeCounts = jdbcTemplate.queryForList(
            "SELECT like_count FROM product ORDER BY id",
            Int::class.java,
        )
        assertAll(
            { assertThat(jobExecution.exitStatus.exitCode).isEqualTo(ExitStatus.COMPLETED.exitCode) },
            { assertThat(likeCounts[0]).isEqualTo(5) },
            { assertThat(likeCounts[1]).isEqualTo(3) },
            { assertThat(likeCounts[2]).isEqualTo(0) },
        )
    }
}
