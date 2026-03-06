package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BrandModel")
class BrandModelTest {

    companion object {
        private const val VALID_NAME = "루프팩"
        private const val VALID_DESCRIPTION = "감성 이커머스 브랜드"
        private const val VALID_LOGO_URL = "https://example.com/logo.png"
    }

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 BrandModel이 생성되고, 기본 상태는 ACTIVE이다")
        @Test
        fun createsBrandModel_whenAllFieldsAreValid() {
            // arrange & act
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )

            // assert
            assertThat(brand.name).isEqualTo(VALID_NAME)
            assertThat(brand.description).isEqualTo(VALID_DESCRIPTION)
            assertThat(brand.logoUrl).isEqualTo(VALID_LOGO_URL)
            assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE)
        }

        @DisplayName("description과 logoUrl이 null이어도 정상 생성된다")
        @Test
        fun createsBrandModel_whenOptionalFieldsAreNull() {
            // arrange & act
            val brand = BrandModel(name = VALID_NAME)

            // assert
            assertThat(brand.name).isEqualTo(VALID_NAME)
            assertThat(brand.description).isNull()
            assertThat(brand.logoUrl).isNull()
            assertThat(brand.status).isEqualTo(BrandStatus.ACTIVE)
        }
    }

    @DisplayName("이름 검증")
    @Nested
    inner class NameValidation {
        @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange & act & assert
            assertThatThrownBy {
                BrandModel(name = "")
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 공백만으로 이루어져 있으면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsOnlyWhitespace() {
            // arrange & act & assert
            assertThatThrownBy {
                BrandModel(name = "   ")
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 100자를 초과하면 예외가 발생한다")
        @Test
        fun throwsException_whenNameExceeds100Characters() {
            // arrange & act & assert
            assertThatThrownBy {
                BrandModel(name = "가".repeat(101))
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 정확히 100자이면 정상 생성된다")
        @Test
        fun createsBrandModel_whenNameIsExactly100Characters() {
            // arrange & act
            val brand = BrandModel(name = "가".repeat(100))

            // assert
            assertThat(brand.name).hasSize(100)
        }
    }

    @DisplayName("update")
    @Nested
    inner class Update {
        @DisplayName("유효한 값으로 update하면 모든 필드가 변경된다")
        @Test
        fun updatesAllFields_whenValidValuesProvided() {
            // arrange
            val brand = BrandModel(
                name = VALID_NAME,
                description = VALID_DESCRIPTION,
                logoUrl = VALID_LOGO_URL,
            )
            val newName = "새로운브랜드"
            val newDescription = "새로운 설명"
            val newLogoUrl = "https://example.com/new-logo.png"

            // act
            brand.update(
                name = newName,
                description = newDescription,
                logoUrl = newLogoUrl,
                status = BrandStatus.INACTIVE,
            )

            // assert
            assertThat(brand.name).isEqualTo(newName)
            assertThat(brand.description).isEqualTo(newDescription)
            assertThat(brand.logoUrl).isEqualTo(newLogoUrl)
            assertThat(brand.status).isEqualTo(BrandStatus.INACTIVE)
        }

        @DisplayName("update 시 이름이 빈 문자열이면 예외가 발생한다")
        @Test
        fun throwsException_whenUpdateNameIsBlank() {
            // arrange
            val brand = BrandModel(name = VALID_NAME)

            // act & assert
            assertThatThrownBy {
                brand.update(
                    name = "",
                    description = null,
                    logoUrl = null,
                    status = BrandStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("update 시 이름이 100자를 초과하면 예외가 발생한다")
        @Test
        fun throwsException_whenUpdateNameExceeds100Characters() {
            // arrange
            val brand = BrandModel(name = VALID_NAME)

            // act & assert
            assertThatThrownBy {
                brand.update(
                    name = "가".repeat(101),
                    description = null,
                    logoUrl = null,
                    status = BrandStatus.ACTIVE,
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("isActive")
    @Nested
    inner class IsActive {
        @DisplayName("상태가 ACTIVE이면 true를 반환한다")
        @Test
        fun returnsTrue_whenStatusIsActive() {
            // arrange
            val brand = BrandModel(name = VALID_NAME, status = BrandStatus.ACTIVE)

            // act & assert
            assertThat(brand.isActive()).isTrue()
        }

        @DisplayName("상태가 INACTIVE이면 false를 반환한다")
        @Test
        fun returnsFalse_whenStatusIsInactive() {
            // arrange
            val brand = BrandModel(name = VALID_NAME, status = BrandStatus.INACTIVE)

            // act & assert
            assertThat(brand.isActive()).isFalse()
        }
    }
}
