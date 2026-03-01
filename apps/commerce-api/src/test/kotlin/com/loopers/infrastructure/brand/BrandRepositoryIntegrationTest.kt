package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@DisplayName("BrandRepository 통합 테스트")
@SpringBootTest
class BrandRepositoryIntegrationTest
@Autowired
constructor(
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val admin = "loopers.admin"

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand.register(name = name), admin)
    }

    @Nested
    @DisplayName("save 시")
    inner class WhenSave {
        @Test
        @DisplayName("저장 후 findById로 조회하면 동일한 브랜드를 반환한다")
        fun save_success() {
            // act
            val saved = createBrand()

            // assert
            val found = brandRepository.findById(saved.id!!)
            assertAll(
                { assertThat(found).isNotNull },
                { assertThat(found!!.name.value).isEqualTo("나이키") },
                { assertThat(found!!.status.name).isEqualTo("INACTIVE") },
            )
        }
    }

    @Nested
    @DisplayName("findById 시")
    inner class WhenFindById {
        @Test
        @DisplayName("존재하는 ID로 조회하면 Brand를 반환한다")
        fun findById_exists() {
            // arrange
            val saved = createBrand()

            // act
            val found = brandRepository.findById(saved.id!!)

            // assert
            assertThat(found).isNotNull
            assertThat(found!!.name.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 null을 반환한다")
        fun findById_notExists() {
            // act
            val found = brandRepository.findById(999L)

            // assert
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("삭제된 브랜드는 조회되지 않는다")
        fun findById_deleted() {
            // arrange
            val saved = createBrand()
            brandRepository.delete(saved.id!!, admin)

            // act
            val found = brandRepository.findById(saved.id!!)

            // assert
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findAll 시")
    inner class WhenFindAll {
        @Test
        @DisplayName("여러 건 조회에 성공한다")
        fun findAll_success() {
            // arrange
            createBrand("나이키")
            createBrand("아디다스")

            // act
            val brands = brandRepository.findAll()

            // assert
            assertThat(brands).hasSize(2)
        }

        @Test
        @DisplayName("삭제된 브랜드는 제외된다")
        fun findAll_excludesDeleted() {
            // arrange
            val brand1 = createBrand("나이키")
            createBrand("아디다스")
            brandRepository.delete(brand1.id!!, admin)

            // act
            val brands = brandRepository.findAll()

            // assert
            assertThat(brands).hasSize(1)
            assertThat(brands[0].name.value).isEqualTo("아디다스")
        }
    }
}
