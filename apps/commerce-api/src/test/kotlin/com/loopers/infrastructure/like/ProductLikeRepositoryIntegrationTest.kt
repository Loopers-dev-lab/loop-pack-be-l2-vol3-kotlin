package com.loopers.infrastructure.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.page.PageRequest
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@DisplayName("ProductLikeRepository 통합 테스트")
@SpringBootTest
class ProductLikeRepositoryIntegrationTest
@Autowired
constructor(
    private val productLikeRepository: ProductLikeRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    platformTransactionManager: PlatformTransactionManager,
) {
    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
    }

    private val transactionTemplate = TransactionTemplate(platformTransactionManager)

    companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2024-01-01T00:00:00Z")
        private const val ADMIN = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun saveLike(
        userId: Long = 1L,
        productId: Long = 100L,
    ): Boolean = productLikeRepository.save(ProductLike.register(userId = userId, productId = productId))

    @Nested
    @DisplayName("save 시")
    inner class WhenSave {
        @Test
        @DisplayName("최초 save는 true를 반환하고 row가 존재한다")
        fun save_first_returnsTrue() {
            val result = saveLike(userId = 1L, productId = 100L)

            assertAll(
                { assertThat(result).isTrue() },
                { assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isTrue() },
            )
        }

        @Test
        @DisplayName("중복 save는 false를 반환하고 row가 1건 유지된다")
        fun save_duplicate_returnsFalse() {
            saveLike(userId = 1L, productId = 100L)

            val result = saveLike(userId = 1L, productId = 100L)

            assertAll(
                { assertThat(result).isFalse() },
                { assertThat(productLikeRepository.countByProductId(100L)).isEqualTo(1) },
            )
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndProductId 시")
    inner class WhenExistsByUserIdAndProductId {
        @Test
        @DisplayName("존재하는 좋아요를 조회하면 true를 반환한다")
        fun exists_true() {
            saveLike(userId = 1L, productId = 100L)

            assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 좋아요를 조회하면 false를 반환한다")
        fun exists_false() {
            assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndProductId 시")
    inner class WhenDeleteByUserIdAndProductId {
        @Test
        @DisplayName("최초 delete는 true를 반환하고 row가 없어진다")
        fun delete_first_returnsTrue() {
            saveLike(userId = 1L, productId = 100L)

            val result = productLikeRepository.deleteByUserIdAndProductId(1L, 100L)

            assertAll(
                { assertThat(result).isTrue() },
                { assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isFalse() },
            )
        }

        @Test
        @DisplayName("존재하지 않는 데이터를 삭제하면 false를 반환한다")
        fun delete_nonExistent_returnsFalse() {
            val result = productLikeRepository.deleteByUserIdAndProductId(999L, 999L)

            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("재 delete는 false를 반환한다")
        fun delete_again_returnsFalse() {
            saveLike(userId = 1L, productId = 100L)
            productLikeRepository.deleteByUserIdAndProductId(1L, 100L)

            val result = productLikeRepository.deleteByUserIdAndProductId(1L, 100L)

            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("createdAt UTC semantics")
    inner class WhenCreatedAtUtcSemantics {
        @Test
        @DisplayName("save 후 DB에서 created_at이 fixed Clock 시각과 일치한다")
        fun save_createdAtMatchesFixedClock() {
            saveLike(userId = 1L, productId = 100L)

            val ts = jdbcTemplate.queryForObject(
                "SELECT created_at FROM product_like WHERE user_id = 1 AND product_id = 100",
                Timestamp::class.java,
            )

            assertThat(ts!!.toInstant()).isEqualTo(FIXED_INSTANT)
        }
    }

    @Nested
    @DisplayName("findAllByUserId 시")
    inner class WhenFindAllByUserId {
        @Test
        @DisplayName("사용자의 좋아요 목록을 페이징하여 조회한다")
        fun findAll_paging() {
            val userId = 1L
            repeat(15) { saveLike(userId = userId, productId = (it + 1).toLong()) }

            val pageRequest = PageRequest().apply { size = 10 }
            val result = productLikeRepository.findAllByUserId(userId, pageRequest)

            assertAll(
                { assertThat(result.content).hasSize(10) },
                { assertThat(result.totalElements).isEqualTo(15L) },
                { assertThat(result.page).isEqualTo(0) },
                { assertThat(result.size).isEqualTo(10) },
            )
        }

        @Test
        @DisplayName("id 기준 내림차순으로 정렬된다")
        fun findAll_sortedByIdDesc() {
            val userId = 1L
            saveLike(userId = userId, productId = 1L)
            saveLike(userId = userId, productId = 2L)
            saveLike(userId = userId, productId = 3L)

            val result = productLikeRepository.findAllByUserId(userId, PageRequest())

            val productIds = result.content.map { it.productId }
            assertThat(productIds).isEqualTo(listOf(3L, 2L, 1L))
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 목록을 반환한다")
        fun findAll_empty() {
            val result = productLikeRepository.findAllByUserId(999L, PageRequest())

            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }
    }

    @Nested
    @DisplayName("countByProductId 시")
    inner class WhenCountByProductId {
        @Test
        @DisplayName("상품의 좋아요 수를 정확히 반환한다")
        fun count_multiple() {
            val productId = 100L
            saveLike(userId = 1L, productId = productId)
            saveLike(userId = 2L, productId = productId)
            saveLike(userId = 3L, productId = productId)

            assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(3)
        }

        @Test
        @DisplayName("좋아요가 없는 상품은 0을 반환한다")
        fun count_zero() {
            assertThat(productLikeRepository.countByProductId(999L)).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("트랜잭션 commit 경로 통합 테스트")
    inner class WhenTransactionCommit {
        @Test
        @DisplayName("save(최초) → save(중복) → incrementLikeCount() 한 트랜잭션 — UnexpectedRollbackException 없이 commit 성공, row=1, likeCount 증가")
        fun saveDuplicate_thenIncrement_commitSucceeds() {
            val brand = brandRepository.save(Brand.register(name = "브랜드"), ADMIN)
            val activeBrand = brandRepository.save(brand.update("브랜드", "ACTIVE"), ADMIN)
            val product = Product.register(
                name = "상품",
                regularPrice = Money(BigDecimal.valueOf(10000)),
                sellingPrice = Money(BigDecimal.valueOf(10000)),
                brandId = activeBrand.id!!,
            )
            val saved = productRepository.save(product, ADMIN)
            val activeProductId = productRepository.save(saved.activate(), ADMIN).id!!
            val userId = 1L

            assertDoesNotThrow {
                transactionTemplate.execute {
                    productLikeRepository.save(ProductLike.register(userId, activeProductId))
                    productLikeRepository.save(ProductLike.register(userId, activeProductId))
                    productRepository.incrementLikeCount(activeProductId)
                }
            }

            assertAll(
                { assertThat(productLikeRepository.countByProductId(activeProductId)).isEqualTo(1) },
                { assertThat(productRepository.findById(activeProductId)!!.likeCount).isEqualTo(1) },
            )
        }
    }
}
