package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.cache.ProductCacheRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class ProductFacadeCacheIntegrationTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productCacheRepository: ProductCacheRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @DisplayName("상품 상세 조회 캐시 테스트")
    @Nested
    inner class GetProductCache {
        @DisplayName("처음 조회 시 DB에서 가져오고, 캐시에 저장한다.")
        @Test
        fun cachesProductOnFirstQuery() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productJpaRepository.save(
                Product(
                    brandId = brand.id,
                    name = "에어맥스",
                    description = "러닝화",
                    price = 150000L,
                    stockQuantity = 100,
                ),
            )

            // act
            productFacade.getProduct(product.id)

            // assert
            val cached = productCacheRepository.get(product.id)
            assertAll(
                { assertThat(cached).isNotNull() },
                { assertThat(cached!!.name).isEqualTo("에어맥스") },
                { assertThat(cached!!.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("두 번째 조회 시 캐시에서 가져온다.")
        @Test
        fun returnsFromCacheOnSecondQuery() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productJpaRepository.save(
                Product(
                    brandId = brand.id,
                    name = "에어맥스",
                    description = "러닝화",
                    price = 150000L,
                    stockQuantity = 100,
                ),
            )
            productFacade.getProduct(product.id)

            // act
            val result = productFacade.getProduct(product.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(product.id) },
                { assertThat(result.name).isEqualTo("에어맥스") },
            )
        }

        @DisplayName("상품 수정 후 조회하면, 캐시가 무효화되어 최신 데이터를 반환한다.")
        @Test
        fun returnsUpdatedData_afterProductUpdate() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productJpaRepository.save(
                Product(
                    brandId = brand.id,
                    name = "에어맥스",
                    description = "러닝화",
                    price = 150000L,
                    stockQuantity = 100,
                ),
            )
            productFacade.getProduct(product.id)

            // act
            productFacade.updateProduct(product.id, "에어맥스2", "새 러닝화", 200000L, 50)
            val result = productFacade.getProduct(product.id)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어맥스2") },
                { assertThat(result.price).isEqualTo(200000L) },
            )
        }

        @DisplayName("상품 삭제 후 캐시가 무효화된다.")
        @Test
        fun evictsCache_afterProductDelete() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productJpaRepository.save(
                Product(
                    brandId = brand.id,
                    name = "에어맥스",
                    description = "러닝화",
                    price = 150000L,
                    stockQuantity = 100,
                ),
            )
            productFacade.getProduct(product.id)

            // act
            productFacade.deleteProduct(product.id)

            // assert
            val cached = productCacheRepository.get(product.id)
            assertThat(cached).isNull()
        }
    }
}
