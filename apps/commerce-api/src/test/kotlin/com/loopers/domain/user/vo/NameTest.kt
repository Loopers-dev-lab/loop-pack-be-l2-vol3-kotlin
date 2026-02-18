package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {
    @DisplayName("이름을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 이름으로 생성하면 Name 객체를 반환한다")
        @Test
        fun createsName_whenValidNameIsProvided() {
            // arrange
            val validName = "테스트"

            // act
            val name = Name.of(validName)

            // assert
            assertThat(name.value).isEqualTo(validName)
        }

        @DisplayName("영문 이름이 유효하면 Name 객체를 반환한다")
        @Test
        fun createsName_whenValidEnglishNameIsProvided() {
            // arrange
            val validName = "John Doe"

            // act
            val name = Name.of(validName)

            // assert
            assertThat(name.value).isEqualTo(validName)
        }

        @DisplayName("이름이 비어있으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenNameIsEmpty() {
            // arrange
            val emptyName = ""

            // act
            val result = assertThrows<CoreException> {
                Name.of(emptyName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 1자이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenNameIsSingleCharacter() {
            // arrange
            val singleCharName = "a"

            // act
            val result = assertThrows<CoreException> {
                Name.of(singleCharName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 공백만 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenNameIsOnlyWhitespace() {
            // arrange
            val whitespaceOnlyName = "  "

            // act
            val result = assertThrows<CoreException> {
                Name.of(whitespaceOnlyName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 2자이면 성공한다")
        @Test
        fun createsName_whenNameIsTwoCharacters() {
            // arrange
            val twoCharName = "김철중"

            // act
            val name = Name.of(twoCharName)

            // assert
            assertThat(name.value).isEqualTo(twoCharName)
        }

        @DisplayName("이름에 숫자가 포함되어도 성공한다")
        @Test
        fun createsName_whenNameContainsNumbers() {
            // arrange
            val nameWithNumbers = "John123"

            // act
            val name = Name.of(nameWithNumbers)

            // assert
            assertThat(name.value).isEqualTo(nameWithNumbers)
        }

        @DisplayName("이름에 특수문자가 포함되어도 성공한다")
        @Test
        fun createsName_whenNameContainsSpecialCharacters() {
            // arrange
            val nameWithSpecial = "Jean-Pierre"

            // act
            val name = Name.of(nameWithSpecial)

            // assert
            assertThat(name.value).isEqualTo(nameWithSpecial)
        }

        @DisplayName("이름에 여러 공백이 포함되어도 성공한다")
        @Test
        fun createsName_whenNameContainsMultipleSpaces() {
            // arrange
            val nameWithMultipleSpaces = "Mary Jane Watson"

            // act
            val name = Name.of(nameWithMultipleSpaces)

            // assert
            assertThat(name.value).isEqualTo(nameWithMultipleSpaces)
        }

        @DisplayName("이름이 탭 문자만 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenNameContainsOnlyTabCharacters() {
            // arrange
            val tabOnlyName = "\t\t"

            // act
            val result = assertThrows<CoreException> {
                Name.of(tabOnlyName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 줄바꿈 문자만 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenNameContainsOnlyNewlineCharacters() {
            // arrange
            val newlineOnlyName = "\n\n"

            // act
            val result = assertThrows<CoreException> {
                Name.of(newlineOnlyName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
