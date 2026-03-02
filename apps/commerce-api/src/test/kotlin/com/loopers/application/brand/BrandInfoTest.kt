package com.loopers.application.brand

import com.loopers.domain.brand.Address
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BusinessNumber
import com.loopers.domain.brand.Description
import com.loopers.domain.brand.Email
import com.loopers.domain.brand.LogoImageUrl
import com.loopers.domain.brand.Name
import com.loopers.domain.brand.PhoneNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class BrandInfoTest {

    private fun setField(obj: Any, fieldName: String, value: Any) {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
    }

    @Test
    fun `BrandModel에서 BrandInfo로 변환 시 모든 필드가 올바르게 매핑된다`() {
        // given
        val now = ZonedDateTime.now()
        val brandModel = BrandModel(
            name = Name("Nike"),
            logoImageUrl = LogoImageUrl("logo.png"),
            description = Description("Nike 브랜드"),
            address = Address("12345", "서울특별시 중구 명동길 14", "1층"),
            email = Email("nike@google.com"),
            phoneNumber = PhoneNumber("02-3783-4401"),
            businessNumber = BusinessNumber("123-45-67890"),
        )
        setField(brandModel, "createdAt", now)
        setField(brandModel, "updatedAt", now)

        // when
        val brandInfo = BrandInfo.from(brandModel)

        // then
        assertThat(brandInfo.id).isEqualTo(brandModel.id)
        assertThat(brandInfo.name).isEqualTo("Nike")
        assertThat(brandInfo.logoImageUrl).isEqualTo("logo.png")
        assertThat(brandInfo.description).isEqualTo("Nike 브랜드")
        assertThat(brandInfo.zipCode).isEqualTo("12345")
        assertThat(brandInfo.roadAddress).isEqualTo("서울특별시 중구 명동길 14")
        assertThat(brandInfo.detailAddress).isEqualTo("1층")
        assertThat(brandInfo.email).isEqualTo("nike@google.com")
        assertThat(brandInfo.phoneNumber).isEqualTo("02-3783-4401")
        assertThat(brandInfo.businessNumber).isEqualTo("123-45-67890")
        assertThat(brandInfo.createdAt).isEqualTo(now)
        assertThat(brandInfo.updatedAt).isEqualTo(now)
    }
}
