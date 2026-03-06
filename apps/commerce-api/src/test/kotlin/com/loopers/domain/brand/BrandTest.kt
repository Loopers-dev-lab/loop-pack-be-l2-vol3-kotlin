package com.loopers.domain.brand

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class BrandTest {
    @DisplayName("브랜드를 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("이름과 설명이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsBrand_whenNameAndDescriptionAreProvided() {
            // arrange
            val name = "나이키"
            val description = "스포츠 브랜드"

            // act
            val brand = Brand(name = name, description = description)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isEqualTo(description) },
            )
        }
    }

    @DisplayName("브랜드를 수정할 때, ")
    @Nested
    inner class Update {
        @DisplayName("이름과 설명이 주어지면, 해당 값으로 수정된다.")
        @Test
        fun updatesBrand_whenNameAndDescriptionAreProvided() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            val newName = "아디다스"
            val newDescription = "독일 스포츠 브랜드"

            // act
            brand.update(name = newName, description = newDescription)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(newName) },
                { assertThat(brand.description).isEqualTo(newDescription) },
            )
        }
    }
}
