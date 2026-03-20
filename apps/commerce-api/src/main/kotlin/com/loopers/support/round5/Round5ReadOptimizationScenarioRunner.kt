package com.loopers.support.round5

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.min

@ConditionalOnProperty(prefix = "round5.read-optimization", name = ["enabled"], havingValue = "true")
@Component
class Round5ReadOptimizationScenarioRunner(
    private val scenarioService: Round5ReadOptimizationScenarioService,
    @Value("\${round5.read-optimization.total-products:100000}") private val totalProducts: Int,
    @Value("\${round5.read-optimization.brand-count:50}") private val brandCount: Int,
    @Value("\${round5.read-optimization.batch-size:1000}") private val batchSize: Int,
    @Value("\${round5.read-optimization.measurement-runs:15}") private val measurementRuns: Int,
    @Value("\${round5.read-optimization.target-brand-index:0}") private val targetBrandIndex: Int,
    @Value("\${round5.read-optimization.dataset-label:round5}") private val datasetLabel: String,
    @Value("\${round5.read-optimization.evidence-dir:.sisyphus/evidence}") private val evidenceDir: String,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        scenarioService.run(
            command = Round5ReadOptimizationScenarioCommand(
                totalProducts = totalProducts,
                brandCount = brandCount,
                batchSize = batchSize,
                measurementRuns = measurementRuns,
                targetBrandIndex = targetBrandIndex,
                datasetLabel = datasetLabel,
                evidenceDir = evidenceDir,
            ),
        )
    }
}

data class Round5ReadOptimizationScenarioCommand(
    val totalProducts: Int,
    val brandCount: Int,
    val batchSize: Int,
    val measurementRuns: Int,
    val targetBrandIndex: Int,
    val datasetLabel: String,
    val evidenceDir: String,
)

@Component
class Round5ReadOptimizationScenarioService(
    private val brandJpaRepository: com.loopers.infrastructure.brand.BrandJpaRepository,
    private val transactionManager: PlatformTransactionManager,
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val transactionTemplate: TransactionTemplate by lazy { TransactionTemplate(transactionManager) }

    fun run(command: Round5ReadOptimizationScenarioCommand) {
        val brands = ensureBrands(command)
        val seededProductCount = seedProducts(command, brands)
        val targetBrandId = brands[command.targetBrandIndex.mod(brands.size)].id
        val evidenceDirectory = prepareEvidenceDirectory(command.evidenceDir)

        val beforeExplain = explain(targetBrandId, useIndex = false)
        val afterExplain = explain(targetBrandId, useIndex = true)
        val beforeMillis = measure(targetBrandId, useIndex = false, runs = command.measurementRuns)
        val afterMillis = measure(targetBrandId, useIndex = true, runs = command.measurementRuns)

        Files.writeString(
            evidenceDirectory.resolve("task-t1-seed-summary.txt"),
            buildSeedSummary(command, brands, seededProductCount, targetBrandId),
        )
        Files.writeString(
            evidenceDirectory.resolve("task-t4-explain.txt"),
            buildExplainSummary(beforeExplain, afterExplain),
        )
        Files.writeString(
            evidenceDirectory.resolve("task-t4-performance.txt"),
            buildPerformanceSummary(beforeMillis, afterMillis),
        )
    }

    private fun ensureBrands(command: Round5ReadOptimizationScenarioCommand): List<com.loopers.domain.brand.BrandModel> = (
        0 until
        command.brandCount
    ).map { index ->
            val name = "round5-${command.datasetLabel}-brand-$index"
            brandJpaRepository.findByNameAndDeletedAtIsNull(name)
                ?: brandJpaRepository.save(
                    com.loopers.domain.brand.BrandModel(
                        name = name,
                        description = "Round5 brand $index",
                    ),
                )
        }

    private fun seedProducts(
        command: Round5ReadOptimizationScenarioCommand,
        brands: List<com.loopers.domain.brand.BrandModel>,
    ): Long {
        val existingCount = countSeededProducts(command.datasetLabel)
        if (existingCount >= command.totalProducts.toLong()) {
            return existingCount
        }

        var inserted = existingCount
        while (inserted < command.totalProducts.toLong()) {
            val start = inserted.toInt()
            val endExclusive = min(command.totalProducts, start + command.batchSize)
            transactionTemplate.executeWithoutResult {
                for (index in start until endExclusive) {
                    val brand = brands[index % brands.size]
                    entityManager.persist(
                        com.loopers.domain.product.ProductModel(
                            name = seededProductName(command.datasetLabel, index),
                            price = 10_000L + ((index * 97L) % 90_000L),
                            brandId = brand.id,
                            description = "Round5 product $index for ${brand.name}",
                            thumbnailImageUrl = "https://example.com/round5/${command.datasetLabel}/$index.png",
                            stockQuantity = 50 + (index % 300),
                            likesCount = ((index * 13L) % 500L),
                            saleStatus = com.loopers.domain.product.SaleStatus.SELLING,
                            displayStatus = com.loopers.domain.product.DisplayStatus.VISIBLE,
                        ),
                    )
                }
                entityManager.flush()
                entityManager.clear()
            }
            inserted = endExclusive.toLong()
        }

        return countSeededProducts(command.datasetLabel)
    }

    private fun countSeededProducts(datasetLabel: String): Long {
        val query = entityManager.createNativeQuery(
            """
            SELECT COUNT(*)
            FROM product
            WHERE deleted_at IS NULL
              AND name LIKE ?
            """.trimIndent(),
        )
        query.setParameter(1, seededProductNamePrefix(datasetLabel) + "%")
        return (query.singleResult as Number).toLong()
    }

    private fun explain(targetBrandId: Long, useIndex: Boolean): String {
        val rows = transactionTemplate.execute {
            entityManager.createNativeQuery(explainSql(useIndex))
                .setParameter(1, targetBrandId)
                .resultList
        }.orEmpty()

        return rows.joinToString(separator = System.lineSeparator()) { row ->
            when (row) {
                is Array<*> -> row.joinToString(" | ") { it.toString() }
                else -> row.toString()
            }
        }
    }

    private fun measure(targetBrandId: Long, useIndex: Boolean, runs: Int): Double {
        repeat(3) {
            transactionTemplate.executeWithoutResult {
                entityManager.createNativeQuery(selectSql(useIndex))
                    .setParameter(1, targetBrandId)
                    .resultList
            }
        }

        val startedAt = System.nanoTime()
        repeat(runs) {
            transactionTemplate.executeWithoutResult {
                entityManager.createNativeQuery(selectSql(useIndex))
                    .setParameter(1, targetBrandId)
                    .resultList
            }
        }
        return (System.nanoTime() - startedAt) / 1_000_000.0 / runs
    }

    private fun prepareEvidenceDirectory(evidenceDir: String): Path {
        val path = Path.of(evidenceDir)
        Files.createDirectories(path)
        return path
    }

    private fun buildSeedSummary(
        command: Round5ReadOptimizationScenarioCommand,
        brands: List<com.loopers.domain.brand.BrandModel>,
        seededProductCount: Long,
        targetBrandId: Long,
    ): String = buildString {
            appendLine("datasetLabel=${command.datasetLabel}")
            appendLine("totalProducts=${command.totalProducts}")
            appendLine("seededProductCount=$seededProductCount")
            appendLine("brandCount=${brands.size}")
            appendLine("targetBrandId=$targetBrandId")
            appendLine("batchSize=${command.batchSize}")
            appendLine("sampleProductPrefix=${seededProductNamePrefix(command.datasetLabel)}")
        }

    private fun buildExplainSummary(beforeExplain: String, afterExplain: String): String = buildString {
            appendLine("[before-ignore-index]")
            appendLine(beforeExplain)
            appendLine()
            appendLine("[after-use-index]")
            appendLine(afterExplain)
        }

    private fun buildPerformanceSummary(beforeMillis: Double, afterMillis: Double): String = buildString {
            appendLine("beforeAverageMillis=${"%.3f".format(beforeMillis)}")
            appendLine("afterAverageMillis=${"%.3f".format(afterMillis)}")
            appendLine("improvementMillis=${"%.3f".format(beforeMillis - afterMillis)}")
            appendLine(
                "improvementPercent=${"%.2f".format(
                    if (beforeMillis == 0.0) 0.0 else ((beforeMillis - afterMillis) / beforeMillis) * 100,
                )}",
            )
        }

    private fun explainSql(useIndex: Boolean): String {
        val indexHint = if (useIndex) "" else "IGNORE INDEX (idx_product_brand_deleted_at_likes_count)"
        return """
            EXPLAIN
            SELECT id, brand_id, likes_count
            FROM product $indexHint
            WHERE brand_id = ?
              AND deleted_at IS NULL
            ORDER BY likes_count DESC, id DESC
            LIMIT 20
            """.trimIndent()
    }

    private fun selectSql(useIndex: Boolean): String {
        val indexHint = if (useIndex) "" else "IGNORE INDEX (idx_product_brand_deleted_at_likes_count)"
        return """
            SELECT id, brand_id, likes_count
            FROM product $indexHint
            WHERE brand_id = ?
              AND deleted_at IS NULL
            ORDER BY likes_count DESC, id DESC
            LIMIT 20
            """.trimIndent()
    }

    private fun seededProductName(datasetLabel: String, index: Int): String =
        seededProductNamePrefix(datasetLabel) + index.toString().padStart(6, '0')

    private fun seededProductNamePrefix(datasetLabel: String): String = "round5-$datasetLabel-product-"
}
