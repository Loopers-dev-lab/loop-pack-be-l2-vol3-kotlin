package com.loopers.seeder

import com.loopers.domain.catalog.ProductModel
import jakarta.persistence.EntityManager
import net.datafaker.Faker
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random

@Component
@Order(3)
@Profile("!test")
class ProductSeeder(
    private val entityManager: EntityManager,
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(ProductSeeder::class.java)
        private const val TOTAL_PRODUCTS = 100_000
        private const val BATCH_SIZE = 1_000
        private const val SOLD_OUT_RATE = 0.10
    }

    @Transactional
    override fun run(vararg args: String?) {
        val count = entityManager
            .createQuery("SELECT COUNT(p) FROM ProductModel p", Long::class.java)
            .singleResult
        if (count > 0) return

        log.info("ProductSeeder: {}개 상품 생성 시작", TOTAL_PRODUCTS)

        val faker = Faker()
        val random = Random(42)
        val brandIds = entityManager
            .createQuery("SELECT b.id FROM BrandModel b", Long::class.java)
            .resultList

        val soldOutCount = (TOTAL_PRODUCTS * SOLD_OUT_RATE).toInt()
        val logMin = ln(10.0)
        val logMax = ln(2000.0)

        for (i in 1..TOTAL_PRODUCTS) {
            val isSoldOut = i <= soldOutCount
            val quantity = if (isSoldOut) 0 else random.nextInt(1, 201)

            val priceValue = exp(logMin + random.nextDouble() * (logMax - logMin)).toInt().coerceIn(10, 2000)

            val product = ProductModel(
                brandId = brandIds[random.nextInt(brandIds.size)],
                name = faker.commerce().productName(),
                quantity = quantity,
                price = BigDecimal(priceValue),
            )
            entityManager.persist(product)

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }
        entityManager.flush()
        entityManager.clear()

        updateLikeCountsWithParetoDistribution(random)

        log.info("ProductSeeder: 완료 (품절 {}개, 재고 {}개)", soldOutCount, TOTAL_PRODUCTS - soldOutCount)
    }

    private fun updateLikeCountsWithParetoDistribution(random: Random) {
        log.info("ProductSeeder: like_count 파레토 분포 업데이트 시작")

        val alpha = 0.8
        val maxLikeCount = 10_000

        val productIds = entityManager
            .createQuery("SELECT p.id FROM ProductModel p ORDER BY p.id", Long::class.java)
            .resultList

        val likeCounts = productIds.map { generateParetoValue(random, alpha, maxLikeCount) }
            .sortedDescending()

        for (batch in productIds.indices.chunked(BATCH_SIZE)) {
            for (i in batch) {
                entityManager.createNativeQuery(
                    "UPDATE products SET like_count = :likeCount WHERE id = :id",
                )
                    .setParameter("likeCount", likeCounts[i])
                    .setParameter("id", productIds[i])
                    .executeUpdate()
            }
            entityManager.flush()
            entityManager.clear()
        }

        log.info("ProductSeeder: like_count 업데이트 완료")
    }

    private fun generateParetoValue(random: Random, alpha: Double, maxValue: Int): Int {
        val u = random.nextDouble()
        val pareto = 1.0 / (1.0 - u).pow(1.0 / alpha)
        return (pareto - 1).toInt().coerceIn(0, maxValue)
    }
}
