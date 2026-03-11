package com.loopers.seeder

import com.loopers.domain.catalog.BrandModel
import jakarta.persistence.EntityManager
import net.datafaker.Faker
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Order(2)
@Profile("!test")
class BrandSeeder(
    private val entityManager: EntityManager,
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(BrandSeeder::class.java)
        private const val TOTAL_BRANDS = 50
    }

    @Transactional
    override fun run(vararg args: String?) {
        val count = entityManager
            .createQuery("SELECT COUNT(b) FROM BrandModel b", Long::class.java)
            .singleResult
        if (count > 0) return

        log.info("BrandSeeder: {}개 브랜드 생성 시작", TOTAL_BRANDS)

        val faker = Faker()
        for (i in 1..TOTAL_BRANDS) {
            entityManager.persist(BrandModel(name = faker.company().name()))
        }
        entityManager.flush()
        entityManager.clear()

        log.info("BrandSeeder: 완료")
    }
}
