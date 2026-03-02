package com.loopers.domain.product

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class LikeCountTest {

    @Test
    fun `유효한 파라미터 입력 시 생성한다`() {
        assertDoesNotThrow {
            LikeCount(1L)
        }
    }

    @Test
    fun `음수 입력 시 에러 반환한다`() {
        assertThrows<IllegalArgumentException> {
            LikeCount(-1L)
        }
    }
}
