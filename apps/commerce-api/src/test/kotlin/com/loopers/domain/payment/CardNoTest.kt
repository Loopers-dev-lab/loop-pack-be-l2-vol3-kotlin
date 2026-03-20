package com.loopers.domain.payment

import com.loopers.domain.payment.vo.CardNo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CardNoTest {
    @DisplayName("카드 번호를 생성할 때,")
    @Nested
    inner class Of {
        @DisplayName("올바른 형식이면, 정상적으로 생성된다.")
        @Test
        fun createsCardNo_whenValidFormat() {
            // act
            val cardNo = CardNo.of("1234-5678-9012-3456")

            // assert
            assertThat(cardNo.value).isEqualTo("1234-5678-9012-3456")
        }

        @DisplayName("하이픈이 없으면, 예외가 발생한다.")
        @Test
        fun throwsException_whenNoHyphens() {
            assertThrows<IllegalArgumentException> { CardNo.of("1234567890123456") }
        }

        @DisplayName("자릿수가 맞지 않으면, 예외가 발생한다.")
        @Test
        fun throwsException_whenWrongDigitCount() {
            assertThrows<IllegalArgumentException> { CardNo.of("123-5678-9012-3456") }
        }

        @DisplayName("문자가 포함되면, 예외가 발생한다.")
        @Test
        fun throwsException_whenContainsLetters() {
            assertThrows<IllegalArgumentException> { CardNo.of("1234-abcd-9012-3456") }
        }

        @DisplayName("빈 문자열이면, 예외가 발생한다.")
        @Test
        fun throwsException_whenEmpty() {
            assertThrows<IllegalArgumentException> { CardNo.of("") }
        }
    }
}
