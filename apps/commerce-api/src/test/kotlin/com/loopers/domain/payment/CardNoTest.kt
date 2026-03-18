package com.loopers.domain.payment

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CardNoTest {

    @Test
    fun `유효한 카드 번호로 생성할 수 있어야 한다`() {
        val cardNo = CardNo(VALID_CARD_NO)

        assertThat(cardNo.value).isEqualTo(VALID_CARD_NO)
    }

    @Test
    fun `하이픈이 없는 카드 번호는 실패해야 한다`() {
        assertThatThrownBy { CardNo("1234567890123456") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `자릿수가 부족한 카드 번호는 실패해야 한다`() {
        assertThatThrownBy { CardNo("1234-5678-9012-345") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `문자가 포함된 카드 번호는 실패해야 한다`() {
        assertThatThrownBy { CardNo("1234-5678-90ab-3456") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `빈 문자열은 실패해야 한다`() {
        assertThatThrownBy { CardNo("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `toString은 중간 자리를 마스킹해야 한다`() {
        val cardNo = CardNo(VALID_CARD_NO)

        assertThat(cardNo.toString()).isEqualTo("1234-****-****-3456")
    }

    companion object {
        private const val VALID_CARD_NO = "1234-5678-9012-3456"
    }
}
