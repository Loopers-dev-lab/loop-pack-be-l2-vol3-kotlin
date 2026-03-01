package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

@DisplayName("Brand 수정")
class BrandUpdateTest {

    @Nested
    @DisplayName("유효한 이름과 상태로 수정하면 성공한다")
    inner class WhenValidUpdate {
        @Test
        @DisplayName("이름과 상태를 동시에 변경한다")
        fun update_nameAndStatus() {
            val brand = Brand.register(name = "나이키")
            val updated = brand.update("아디다스", "ACTIVE")
            assertAll(
                { assertThat(updated.name.value).isEqualTo("아디다스") },
                { assertThat(updated.status).isEqualTo(Brand.Status.ACTIVE) },
            )
        }

        @Test
        @DisplayName("같은 상태로 수정해도 성공한다 (멱등)")
        fun update_sameStatus() {
            val brand = Brand.register(name = "나이키")
            val updated = brand.update("나이키", "INACTIVE")
            assertThat(updated.status).isEqualTo(Brand.Status.INACTIVE)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 이름으로 수정하면 실패한다")
    inner class WhenInvalidName {
        @Test
        @DisplayName("빈 문자열이면 예외를 던진다")
        fun update_emptyName() {
            val brand = Brand.register(name = "나이키")
            val exception = assertThrows<CoreException> { brand.update("", "ACTIVE") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }

        @Test
        @DisplayName("51자 이상이면 예외를 던진다")
        fun update_tooLongName() {
            val brand = Brand.register(name = "나이키")
            val name = "가".repeat(51)
            val exception = assertThrows<CoreException> { brand.update(name, "ACTIVE") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }
    }

    @Nested
    @DisplayName("유효하지 않은 상태로 수정하면 실패한다")
    inner class WhenInvalidStatus {
        @Test
        @DisplayName("존재하지 않는 상태값이면 예외를 던진다")
        fun update_invalidStatus() {
            val brand = Brand.register(name = "나이키")
            val exception = assertThrows<CoreException> { brand.update("나이키", "INVALID") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_STATUS)
        }
    }
}
