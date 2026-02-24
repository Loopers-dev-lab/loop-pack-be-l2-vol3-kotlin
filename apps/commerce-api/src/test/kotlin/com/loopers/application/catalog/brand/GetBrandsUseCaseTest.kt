package com.loopers.application.catalog.brand

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetBrandsUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var useCase: GetBrandsUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        useCase = GetBrandsUseCase(brandRepository)
    }

    @Nested
    @DisplayName("브랜드 목록 조회 시")
    inner class Execute {

        @Test
        @DisplayName("브랜드 목록을 페이징하여 조회한다")
        fun getBrands_returnsPagedResults() {
            // arrange
            brandRepository.save(Brand(name = BrandName("나이키")))
            brandRepository.save(Brand(name = BrandName("아디다스")))
            brandRepository.save(Brand(name = BrandName("뉴발란스")))

            // act
            val result = useCase.execute(0, 2)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(3)
        }
    }
}
