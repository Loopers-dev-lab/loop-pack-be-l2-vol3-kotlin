package com.loopers.interfaces.api.admin.brand

import com.loopers.domain.brand.Brand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminBrandV1Request")
class AdminBrandV1RequestTest {

    @Test
    @DisplayName("Request의 status @Pattern이 Brand.Status의 모든 값을 포함한다")
    fun statusPattern_coversAllBrandStatusValues() {
        val allowedValues = setOf("ACTIVE", "INACTIVE")
        val statusValues = Brand.Status.entries.map { it.name }.toSet()
        assertThat(allowedValues).isEqualTo(statusValues)
    }
}
