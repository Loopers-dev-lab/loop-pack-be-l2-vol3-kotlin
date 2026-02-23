package com.loopers.domain.brand

import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.BrandException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드 등록")
    @Nested
    inner class Register {

        @DisplayName("이름이 주어지면 등록에 성공한다")
        @Test
        fun success() {
            // act
            val brand = brandService.register(name = "나이키")

            // assert
            assertThat(brand.id).isGreaterThan(0)
        }

        @DisplayName("이미 존재하는 활성 브랜드명으로 등록하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            // arrange
            brandService.register(name = "나이키")

            // act & assert
            val exception = assertThrows<BrandException> {
                brandService.register(name = "나이키")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }

        @DisplayName("삭제된 브랜드의 이름으로 재등록할 수 있다")
        @Test
        fun canReuseDeletedBrandName() {
            // arrange
            val original = brandService.register(name = "나이키")
            brandService.delete(original.id)

            // act
            val reused = brandService.register(name = "나이키")

            // assert
            assertThat(reused.id).isNotEqualTo(original.id)
        }
    }

    @DisplayName("브랜드 수정")
    @Nested
    inner class Update {

        @DisplayName("새 이름으로 수정하면 성공한다")
        @Test
        fun success() {
            // arrange
            val brand = brandService.register(name = "나이키")

            // act
            val updated = brandService.update(brand.id, name = "뉴나이키")

            // assert
            assertThat(updated.name).isEqualTo("뉴나이키")
        }

        @DisplayName("다른 활성 브랜드와 같은 이름으로 수정하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            // arrange
            brandService.register(name = "나이키")
            val adidas = brandService.register(name = "아디다스")

            // act & assert
            val exception = assertThrows<BrandException> {
                brandService.update(adidas.id, name = "나이키")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }
    }

    @DisplayName("브랜드 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 soft delete 처리된다")
        @Test
        fun success() {
            // arrange
            val brand = brandService.register(name = "나이키")

            // act
            brandService.delete(brand.id)

            // assert
            val entity = brandJpaRepository.findById(brand.id).get()
            assertThat(entity.deletedAt).isNotNull()
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            // arrange
            val brand = brandService.register(name = "나이키")
            brandService.delete(brand.id)

            // act & assert
            val exception = assertThrows<BrandException> {
                brandService.delete(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }
    }
}
