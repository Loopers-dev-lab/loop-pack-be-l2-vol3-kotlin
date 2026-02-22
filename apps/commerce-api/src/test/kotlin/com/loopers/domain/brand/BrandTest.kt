package com.loopers.domain.brand

import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.BrandException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class BrandTest {

    @DisplayName("브랜드 생성")
    @Nested
    inner class Create {

        @DisplayName("이름이 주어지면 성공한다")
        @Test
        fun success() {
            // act
            val brand = Brand.create(name = "나이키")

            // assert
            assertAll(
                { assertThat(brand.id).isEqualTo(0) },
                { assertThat(brand.name).isEqualTo("나이키") },
                { assertThat(brand.isDeleted()).isFalse() },
            )
        }
    }

    @DisplayName("브랜드 이름 검증 실패")
    @Nested
    inner class FailByName {

        @DisplayName("이름이 빈값이면 실패한다")
        @Test
        fun failWhenNameIsEmpty() {
            val exception = assertThrows<BrandException> {
                Brand.create(name = "")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.INVALID_BRAND_NAME)
        }

        @DisplayName("이름이 공백으로만 이루어져 있으면 실패한다")
        @Test
        fun failWhenNameIsBlank() {
            val exception = assertThrows<BrandException> {
                Brand.create(name = "   ")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.INVALID_BRAND_NAME)
        }

        @DisplayName("이름이 50자를 초과하면 실패한다")
        @Test
        fun failWhenNameTooLong() {
            val longName = "가".repeat(51)

            val exception = assertThrows<BrandException> {
                Brand.create(name = longName)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.INVALID_BRAND_NAME)
        }
    }

    @DisplayName("브랜드 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 isDeleted()가 true를 반환한다")
        @Test
        fun deleteSuccess() {
            // arrange
            val brand = Brand.create(name = "나이키")

            // act
            brand.delete()

            // assert
            assertThat(brand.isDeleted()).isTrue()
        }
    }

    @DisplayName("브랜드 수정")
    @Nested
    inner class Update {

        @DisplayName("이름을 수정할 수 있다")
        @Test
        fun updateSuccess() {
            // arrange
            val brand = Brand.create(name = "나이키")

            // act
            brand.update(name = "뉴나이키")

            // assert
            assertThat(brand.name).isEqualTo("뉴나이키")
        }

        @DisplayName("수정 시 이름이 빈값이면 실패한다")
        @Test
        fun failWhenUpdateNameIsEmpty() {
            val brand = Brand.create(name = "나이키")

            val exception = assertThrows<BrandException> {
                brand.update(name = "")
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.INVALID_BRAND_NAME)
        }

        @DisplayName("수정 시 이름이 50자를 초과하면 실패한다")
        @Test
        fun failWhenUpdateNameTooLong() {
            val brand = Brand.create(name = "나이키")
            val longName = "가".repeat(51)

            val exception = assertThrows<BrandException> {
                brand.update(name = longName)
            }
            assertThat(exception.errorCode).isEqualTo(BrandErrorCode.INVALID_BRAND_NAME)
        }
    }
}
