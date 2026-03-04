package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class BrandTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_브랜드를_생성할_수_있다`() {
            // arrange
            val name = BrandName("나이키")

            // act
            val brand = Brand(name = name)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE) },
            )
        }
    }

    @Nested
    inner class ChangeName {
        @Test
        fun `브랜드명을_변경할_수_있다`() {
            // arrange
            val brand = createBrand()
            val newName = BrandName("아디다스")

            // act
            brand.changeName(newName)

            // assert
            assertThat(brand.name).isEqualTo(newName)
        }
    }

    @Nested
    inner class Deactivate {
        @Test
        fun `활성_브랜드를_비활성화할_수_있다`() {
            // arrange
            val brand = createBrand()

            // act
            brand.deactivate()

            // assert
            assertThat(brand.status).isEqualTo(BrandStatus.INACTIVE)
        }

        @Test
        fun `이미_비활성화된_브랜드를_비활성화하면_예외가_발생한다`() {
            // arrange
            val brand = createBrand()
            brand.deactivate()

            // act
            val result = assertThrows<CoreException> { brand.deactivate() }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BRAND_ALREADY_INACTIVE)
        }
    }

    private fun createBrand(
        name: String = "나이키",
    ): Brand {
        return Brand(name = BrandName(name))
    }
}
