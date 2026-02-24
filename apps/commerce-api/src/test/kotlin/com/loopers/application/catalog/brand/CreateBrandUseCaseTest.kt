package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.FakeBrandRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CreateBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: CreateBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = CreateBrandUseCase(brandRepository)
    }

    @Nested
    @DisplayName("브랜드 생성 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 이름으로 생성하면 Brand가 저장되고 BrandInfo가 반환된다")
        fun createBrand_withValidName_savesAndReturnsBrandInfo() {
            // arrange & act
            val result = useCase.execute("나이키")

            // assert
            assertThat(result.name).isEqualTo("나이키")
            assertThat(result.id).isNotEqualTo(0L)
        }
    }
}
