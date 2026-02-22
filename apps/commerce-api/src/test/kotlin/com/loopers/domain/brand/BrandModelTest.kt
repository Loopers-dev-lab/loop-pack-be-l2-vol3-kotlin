package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class BrandModelTest {
    private fun createBrand(
        name: BrandName = BrandName.of("루퍼스"),
        description: String = "루퍼스 브랜드 설명",
        imageUrl: String = "https://example.com/brand.jpg",
    ) = BrandModel(
        name = name,
        description = description,
        imageUrl = imageUrl,
    )

    @DisplayName("브랜드 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("모든 값이 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsBrandModel_whenAllFieldsAreValid() {
            // act
            val brand = createBrand()

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("루퍼스") },
                { assertThat(brand.description).isEqualTo("루퍼스 브랜드 설명") },
                { assertThat(brand.imageUrl).isEqualTo("https://example.com/brand.jpg") },
                { assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE) },
            )
        }
    }

    @DisplayName("브랜드를 수정할 때,")
    @Nested
    inner class Update {
        @DisplayName("유효한 값이 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesBrand_whenValidFieldsAreProvided() {
            // arrange
            val brand = createBrand()

            // act
            brand.update(
                name = BrandName.of("새 브랜드"),
                description = "새 설명",
                imageUrl = "https://example.com/new.jpg",
            )

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("새 브랜드") },
                { assertThat(brand.description).isEqualTo("새 설명") },
                { assertThat(brand.imageUrl).isEqualTo("https://example.com/new.jpg") },
            )
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class Delete {
        @DisplayName("삭제하면, 상태가 DELETED로 변경된다.")
        @Test
        fun changeStatusToDeleted_whenDeleteIsCalled() {
            // arrange
            val brand = createBrand()

            // act
            brand.delete()

            // assert
            assertAll(
                { assertThat(brand.status).isEqualTo(BrandStatus.DELETED) },
                { assertThat(brand.isDeleted()).isTrue() },
            )
        }
    }
}
