package com.loopers.application.brand

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
class BrandFacadeIntegrationTest @Autowired constructor(
    private val brandFacade: BrandFacade,
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
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))

            // assert
            assertThat(brand.id).isGreaterThan(0)
        }

        @DisplayName("이미 존재하는 활성 브랜드명으로 등록하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            // arrange
            brandFacade.register(BrandCommand.Register(name = "나이키"))

            // act & assert
            val exception = assertThrows<BrandException> {
                brandFacade.register(BrandCommand.Register(name = "나이키"))
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }

        @DisplayName("삭제된 브랜드의 이름으로 재등록할 수 있다")
        @Test
        fun canReuseDeletedBrandName() {
            // arrange
            val original = brandFacade.register(BrandCommand.Register(name = "나이키"))
            brandFacade.delete(original.id)

            // act
            val reused = brandFacade.register(BrandCommand.Register(name = "나이키"))

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
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))

            // act
            val updated = brandFacade.update(BrandCommand.Update(brandId = brand.id, name = "뉴나이키"))

            // assert
            assertThat(updated.name).isEqualTo("뉴나이키")
        }

        @DisplayName("같은 이름으로 수정하면 중복 검증 없이 성공한다")
        @Test
        fun successWhenSameName() {
            // arrange
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))

            // act
            val updated = brandFacade.update(BrandCommand.Update(brandId = brand.id, name = "나이키"))

            // assert
            assertThat(updated.name).isEqualTo("나이키")
        }

        @DisplayName("다른 활성 브랜드와 같은 이름으로 수정하면 DUPLICATE_BRAND_NAME 예외가 발생한다")
        @Test
        fun failWhenNameAlreadyExists() {
            // arrange
            brandFacade.register(BrandCommand.Register(name = "나이키"))
            val adidas = brandFacade.register(BrandCommand.Register(name = "아디다스"))

            // act & assert
            val exception = assertThrows<BrandException> {
                brandFacade.update(BrandCommand.Update(brandId = adidas.id, name = "나이키"))
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME)
        }
    }

    @DisplayName("브랜드 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 조회되지 않는다")
        @Test
        fun success() {
            // arrange
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))

            // act
            brandFacade.delete(brand.id)

            // assert
            val exception = assertThrows<BrandException> {
                brandFacade.getActiveBrand(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenAlreadyDeleted() {
            // arrange
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))
            brandFacade.delete(brand.id)

            // act & assert
            val exception = assertThrows<BrandException> {
                brandFacade.delete(brand.id)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }
    }

    @DisplayName("브랜드 단건 조회")
    @Nested
    inner class GetActiveBrand {

        @DisplayName("존재하는 활성 브랜드를 조회하면 성공한다")
        @Test
        fun success() {
            // arrange
            val brand = brandFacade.register(BrandCommand.Register(name = "나이키"))

            // act
            val result = brandFacade.getActiveBrand(brand.id)

            // assert
            assertThat(result.name).isEqualTo("나이키")
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면 BRAND_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenNotFound() {
            val exception = assertThrows<BrandException> {
                brandFacade.getActiveBrand(999L)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND)
        }
    }

    @DisplayName("브랜드 전체 조회")
    @Nested
    inner class GetAllActiveBrands {

        @DisplayName("활성 브랜드만 조회된다")
        @Test
        fun returnsOnlyActiveBrands() {
            // arrange
            brandFacade.register(BrandCommand.Register(name = "나이키"))
            val adidas = brandFacade.register(BrandCommand.Register(name = "아디다스"))
            brandFacade.delete(adidas.id)

            // act
            val brands = brandFacade.getAllActiveBrands()

            // assert
            assertThat(brands).hasSize(1)
            assertThat(brands[0].name).isEqualTo("나이키")
        }
    }
}
